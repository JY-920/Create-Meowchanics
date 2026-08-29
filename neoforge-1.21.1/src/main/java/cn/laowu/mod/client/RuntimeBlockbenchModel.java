package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight renderer for the cube and mesh elements stored in a Blockbench bbmodel. */
public final class RuntimeBlockbenchModel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, RuntimeBlockbenchModel> CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RuntimeBlockbenchModel> INFLATED_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RuntimeBlockbenchModel> MIRRORED_OUTFIT_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RuntimeBlockbenchModel> PANCAKE_OUTFIT_CACHE =
            new ConcurrentHashMap<>();
    private static final RuntimeBlockbenchModel EMPTY = new RuntimeBlockbenchModel(List.of());

    // Exact element UUIDs read from cat_clothes.bbmodel. The cat-pancake model
    // removes the bottom texture row from its 4 px head. Crop that row and its
    // UVs instead of scaling it, or the three remaining texture rows are
    // visibly squashed. The abdomen intentionally keeps its older compression.
    private static final String PANCAKE_HEAD_SHELL = "9096bccc-2833-1907-767e-1084c8ef00c0";
    private static final String PANCAKE_MUZZLE_SHELL = "dc13c2dc-51bd-f697-e39a-c16455288d46";
    private static final String FLIGHT_HEAD_SHELL = "e6ba4935-ed15-ee51-bea3-6c42b72d87eb";
    // The translucent rear shoes in honey_suit.bbmodel sit only a tenth of a
    // pixel above the flattened hind legs. Pancake leg remapping can make one
    // of those planes coplanar at oblique camera angles, so give both shoes a
    // tiny, UV-preserving clearance in the pancake-only model.
    private static final String HONEY_LEFT_HIND_SHOE = "8863ab8f-c4dc-0288-faaf-da1c5aecda85";
    private static final String HONEY_RIGHT_HIND_SHOE = "e92feece-5dcb-347f-b47a-b326e468c609";
    // The only supplied outfit mesh with a compound Euler rotation. It is the
    // fish on the cat's right side. Blockbench's actual scene object for this
    // mesh uses XYZ order, so preserve that order instead of the legacy
    // RuntimeBlockbenchModel order used by older assets.
    private static final String FISHING_RIGHT_FISH = "7a93e707-a768-2d9a-989e-1fccc7045619";
    private static final int OUTFIT_TEXTURE_INDEX = 1;
    private static final float OUTFIT_TEXTURE_SIZE = 64.0F;

    private final List<GroupDef> roots;

    private RuntimeBlockbenchModel(List<GroupDef> roots) {
        this.roots = roots;
    }

    public static RuntimeBlockbenchModel get(ResourceLocation location) {
        return CACHE.computeIfAbsent(location, key -> load(key, false));
    }

    /**
     * Loads a Java-format Blockbench model while applying each cube's inflate
     * value. Existing runtime models retain their historical behaviour through
     * {@link #get(ResourceLocation)}; this path is used by models whose exact
     * authored silhouette depends on inflation, such as the cat helmet.
     */
    public static RuntimeBlockbenchModel getInflated(ResourceLocation location) {
        return INFLATED_CACHE.computeIfAbsent(location, key -> load(key, true));
    }

    /**
     * Loads a cat outfit in Blockbench's visual left/right orientation.
     *
     * <p>LivingEntityRenderer mirrors its model space on X before rendering.
     * Java ModelPart geometry is authored for that convention, while the five
     * supplied free-format Blockbench projects are authored exactly as seen in
     * Blockbench. Reflecting their model data once here prevents every
     * asymmetric accessory from appearing on the opposite side in game. The
     * left/right bone names are exchanged at the same time so walking
     * animations still come from the leg underneath the reflected geometry.</p>
     */
    public static RuntimeBlockbenchModel getCatOutfit(ResourceLocation location) {
        return MIRRORED_OUTFIT_CACHE.computeIfAbsent(location,
                key -> load(key, false).mirrorAcrossX());
    }

    /** Returns the mirrored outfit with geometry and UVs cropped exactly like cat_pancake.bbmodel. */
    public static RuntimeBlockbenchModel getPancakeOutfit(ResourceLocation location) {
        return PANCAKE_OUTFIT_CACHE.computeIfAbsent(location,
                key -> load(key, false).mirrorAcrossX().cropForPancake());
    }

    public static void clearCache() {
        CACHE.clear();
        INFLATED_CACHE.clear();
        MIRRORED_OUTFIT_CACHE.clear();
        PANCAKE_OUTFIT_CACHE.clear();
    }

    private RuntimeBlockbenchModel mirrorAcrossX() {
        List<GroupDef> mirroredRoots = new ArrayList<>(roots.size());
        for (GroupDef root : roots) mirroredRoots.add(mirrorGroupAcrossX(root));
        return new RuntimeBlockbenchModel(List.copyOf(mirroredRoots));
    }

    private static GroupDef mirrorGroupAcrossX(GroupDef group) {
        List<ElementDef> elements = new ArrayList<>(group.elements.size());
        for (ElementDef element : group.elements) elements.add(mirrorElementAcrossX(element));

        List<GroupDef> children = new ArrayList<>(group.children.size());
        for (GroupDef child : group.children) children.add(mirrorGroupAcrossX(child));
        return new GroupDef(mirrorLateralGroupName(group.name), mirrorPositionAcrossX(group.origin),
                mirrorRotationAcrossX(group.rotation), List.copyOf(elements), List.copyOf(children));
    }

    private static ElementDef mirrorElementAcrossX(ElementDef element) {
        List<FaceDef> faces = new ArrayList<>(element.faces.size());
        for (FaceDef face : element.faces) {
            List<VertexDef> vertices = new ArrayList<>(face.vertices.size());
            // A reflection reverses winding. Iterate backwards so lighting
            // normals continue to point out of the reflected surface.
            for (int index = face.vertices.size() - 1; index >= 0; index--) {
                VertexDef vertex = face.vertices.get(index);
                vertices.add(new VertexDef(mirrorPositionAcrossX(vertex.position),
                        vertex.u, vertex.v));
            }
            faces.add(new FaceDef(face.textureIndex, List.copyOf(vertices)));
        }
        return new ElementDef(element.uuid, mirrorPositionAcrossX(element.origin),
                mirrorRotationAcrossX(element.rotation), element.localMeshVertices,
                List.copyOf(faces));
    }

    private static Vec mirrorPositionAcrossX(Vec value) {
        return new Vec(-value.x, value.y, value.z);
    }

    private static Vec mirrorRotationAcrossX(Vec value) {
        return new Vec(value.x, -value.y, -value.z);
    }

    private static String mirrorLateralGroupName(String name) {
        if (name.startsWith("left_")) return "right_" + name.substring("left_".length());
        if (name.startsWith("right_")) return "left_" + name.substring("right_".length());
        return name;
    }

    private RuntimeBlockbenchModel cropForPancake() {
        List<GroupDef> croppedRoots = new ArrayList<>(roots.size());
        for (GroupDef root : roots) croppedRoots.add(cropPancakeGroup(root));
        return new RuntimeBlockbenchModel(List.copyOf(croppedRoots));
    }

    private static GroupDef cropPancakeGroup(GroupDef group) {
        List<ElementDef> elements = new ArrayList<>(group.elements.size());
        for (ElementDef element : group.elements) {
            if (PANCAKE_HEAD_SHELL.equals(element.uuid)
                    || PANCAKE_MUZZLE_SHELL.equals(element.uuid)) {
                elements.add(cropElementEdge(element, CropEdge.MINIMUM, 1.0F));
            } else if (HONEY_LEFT_HIND_SHOE.equals(element.uuid)
                    || HONEY_RIGHT_HIND_SHOE.equals(element.uuid)) {
                elements.add(inflateMeshElement(element, 0.04F));
            } else {
                elements.add(element);
            }
        }

        List<GroupDef> children = new ArrayList<>(group.children.size());
        for (GroupDef child : group.children) children.add(cropPancakeGroup(child));
        return new GroupDef(group.name, group.origin, group.rotation,
                List.copyOf(elements), List.copyOf(children));
    }

    private static ElementDef inflateMeshElement(ElementDef element, float pixels) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (FaceDef face : element.faces) {
            for (VertexDef vertex : face.vertices) {
                minX = Math.min(minX, vertex.position.x);
                minY = Math.min(minY, vertex.position.y);
                minZ = Math.min(minZ, vertex.position.z);
                maxX = Math.max(maxX, vertex.position.x);
                maxY = Math.max(maxY, vertex.position.y);
                maxZ = Math.max(maxZ, vertex.position.z);
            }
        }
        if (!Float.isFinite(minX) || !Float.isFinite(maxX)) return element;

        List<FaceDef> faces = new ArrayList<>(element.faces.size());
        for (FaceDef face : element.faces) {
            List<VertexDef> vertices = new ArrayList<>(face.vertices.size());
            for (VertexDef vertex : face.vertices) {
                Vec position = vertex.position;
                vertices.add(new VertexDef(new Vec(
                        expandEdge(position.x, minX, maxX, pixels),
                        expandEdge(position.y, minY, maxY, pixels),
                        expandEdge(position.z, minZ, maxZ, pixels)),
                        vertex.u, vertex.v));
            }
            faces.add(new FaceDef(face.textureIndex, List.copyOf(vertices)));
        }
        return new ElementDef(element.uuid, element.origin, element.rotation,
                element.localMeshVertices, List.copyOf(faces));
    }

    private static float expandEdge(float value, float minimum, float maximum, float pixels) {
        if (near(value, minimum)) return value - pixels;
        if (near(value, maximum)) return value + pixels;
        return value;
    }

    /**
     * Moves only the removed edge of a mesh and crops the same number of UV
     * pixels toward the opposite edge. End-cap UVs stay untouched, matching a
     * literal Blockbench row deletion rather than a rescale operation.
     */
    private static ElementDef cropElementEdge(ElementDef element, CropEdge edge, float pixels) {
        float minimum = Float.POSITIVE_INFINITY;
        float maximum = Float.NEGATIVE_INFINITY;
        for (FaceDef face : element.faces) {
            if (face.textureIndex != OUTFIT_TEXTURE_INDEX) continue;
            for (VertexDef vertex : face.vertices) {
                minimum = Math.min(minimum, vertex.position.y);
                maximum = Math.max(maximum, vertex.position.y);
            }
        }
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum)) return element;

        float movedEdge = edge == CropEdge.MINIMUM ? minimum : maximum;
        float oppositeEdge = edge == CropEdge.MINIMUM ? maximum : minimum;
        List<FaceDef> faces = new ArrayList<>(element.faces.size());
        for (FaceDef face : element.faces) {
            if (face.textureIndex != OUTFIT_TEXTURE_INDEX) {
                faces.add(face);
                continue;
            }

            boolean spansBothEdges = containsY(face.vertices, movedEdge)
                    && containsY(face.vertices, oppositeEdge);
            float movedU = averageUv(face.vertices, movedEdge, true);
            float movedV = averageUv(face.vertices, movedEdge, false);
            float oppositeU = averageUv(face.vertices, oppositeEdge, true);
            float oppositeV = averageUv(face.vertices, oppositeEdge, false);
            boolean cropU = spansBothEdges
                    && Math.abs(oppositeU - movedU) > Math.abs(oppositeV - movedV);

            List<VertexDef> vertices = new ArrayList<>(face.vertices.size());
            for (VertexDef vertex : face.vertices) {
                if (!near(vertex.position.y, movedEdge)) {
                    vertices.add(vertex);
                    continue;
                }

                float y = vertex.position.y + (edge == CropEdge.MINIMUM ? pixels : -pixels);
                float u = vertex.u;
                float v = vertex.v;
                if (spansBothEdges) {
                    if (cropU) {
                        u += Math.signum(oppositeU - movedU) * pixels / OUTFIT_TEXTURE_SIZE;
                    } else {
                        v += Math.signum(oppositeV - movedV) * pixels / OUTFIT_TEXTURE_SIZE;
                    }
                }
                vertices.add(new VertexDef(
                        new Vec(vertex.position.x, y, vertex.position.z), u, v));
            }
            faces.add(new FaceDef(face.textureIndex, List.copyOf(vertices)));
        }
        return new ElementDef(element.uuid, element.origin, element.rotation,
                element.localMeshVertices, List.copyOf(faces));
    }

    private static boolean containsY(List<VertexDef> vertices, float y) {
        for (VertexDef vertex : vertices) if (near(vertex.position.y, y)) return true;
        return false;
    }

    private static float averageUv(List<VertexDef> vertices, float y, boolean uAxis) {
        float sum = 0.0F;
        int count = 0;
        for (VertexDef vertex : vertices) {
            if (!near(vertex.position.y, y)) continue;
            sum += uAxis ? vertex.u : vertex.v;
            count++;
        }
        return count == 0 ? 0.0F : sum / count;
    }

    private static boolean near(float first, float second) {
        return Math.abs(first - second) < 1.0E-4F;
    }

    private static RuntimeBlockbenchModel load(ResourceLocation location, boolean applyInflate) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location);
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("minecraft:geometry")) return readBedrockGeometry(root);
                float defaultWidth = root.getAsJsonObject("resolution").get("width").getAsFloat();
                float defaultHeight = root.getAsJsonObject("resolution").get("height").getAsFloat();
                List<TextureSize> textures = readTextures(root, defaultWidth, defaultHeight);

                Map<String, ElementDef> elements = new HashMap<>();
                for (JsonElement raw : root.getAsJsonArray("elements")) {
                    ElementDef element = readElement(raw.getAsJsonObject(), textures,
                            defaultWidth, defaultHeight, applyInflate);
                    elements.put(element.uuid, element);
                }

                Map<String, GroupMeta> groups = new HashMap<>();
                JsonArray groupArray = root.has("groups") ? root.getAsJsonArray("groups") : new JsonArray();
                for (JsonElement raw : groupArray) {
                    JsonObject group = raw.getAsJsonObject();
                    groups.put(group.get("uuid").getAsString(), new GroupMeta(
                            string(group, "name", group.get("uuid").getAsString()),
                            vector(group, "origin"), vector(group, "rotation")));
                }

                List<GroupDef> roots = new ArrayList<>();
                for (JsonElement raw : root.getAsJsonArray("outliner")) {
                    if (raw.isJsonObject()) roots.add(readGroup(raw.getAsJsonObject(), groups, elements));
                }
                return new RuntimeBlockbenchModel(List.copyOf(roots));
            }
        } catch (Exception exception) {
            LOGGER.error("Could not load Blockbench model {}", location, exception);
            return EMPTY;
        }
    }

    /** Reads Bedrock geometry exported as model.geo.json without flattening its bone hierarchy. */
    private static RuntimeBlockbenchModel readBedrockGeometry(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) return EMPTY;

        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        float textureWidth = number(description, "texture_width", 64.0F);
        float textureHeight = number(description, "texture_height", 64.0F);
        Map<String, BedrockBone> bones = new LinkedHashMap<>();

        JsonArray boneArray = geometry.getAsJsonArray("bones");
        if (boneArray == null) return EMPTY;
        for (JsonElement rawBone : boneArray) {
            JsonObject bone = rawBone.getAsJsonObject();
            String name = string(bone, "name", "bone");
            String parent = string(bone, "parent", "");
            Vec pivot = vector(bone, "pivot");
            Vec rotation = vector(bone, "rotation");
            List<ElementDef> cubes = new ArrayList<>();
            if (bone.has("cubes")) {
                int index = 0;
                for (JsonElement rawCube : bone.getAsJsonArray("cubes")) {
                    cubes.add(readBedrockCube(rawCube.getAsJsonObject(), pivot,
                            textureWidth, textureHeight, name + ":" + index++));
                }
            }
            bones.put(name, new BedrockBone(name, parent, pivot, rotation, List.copyOf(cubes)));
        }

        List<GroupDef> roots = new ArrayList<>();
        for (BedrockBone bone : bones.values()) {
            if (bone.parent.isEmpty() || !bones.containsKey(bone.parent)) {
                roots.add(readBedrockBone(bone, bones));
            }
        }
        return new RuntimeBlockbenchModel(List.copyOf(roots));
    }

    private static GroupDef readBedrockBone(BedrockBone bone, Map<String, BedrockBone> bones) {
        List<GroupDef> children = new ArrayList<>();
        for (BedrockBone candidate : bones.values()) {
            if (candidate.parent.equals(bone.name)) children.add(readBedrockBone(candidate, bones));
        }
        return new GroupDef(bone.name, bone.pivot, bone.rotation,
                bone.cubes, List.copyOf(children));
    }

    private static ElementDef readBedrockCube(JsonObject cube, Vec bonePivot,
                                              float textureWidth, float textureHeight,
                                              String id) {
        Vec min = vector(cube, "origin");
        Vec size = vector(cube, "size");
        float inflate = number(cube, "inflate", 0.0F);
        min = new Vec(min.x - inflate, min.y - inflate, min.z - inflate);
        Vec max = new Vec(min.x + size.x + inflate * 2.0F,
                min.y + size.y + inflate * 2.0F,
                min.z + size.z + inflate * 2.0F);
        Vec pivot = cube.has("pivot") ? vector(cube, "pivot") : bonePivot;
        Vec rotation = vector(cube, "rotation");
        List<FaceDef> faces = new ArrayList<>();

        JsonElement uvElement = cube.get("uv");
        if (uvElement != null && uvElement.isJsonObject()) {
            for (var entry : uvElement.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject face = entry.getValue().getAsJsonObject();
                JsonArray uv = face.getAsJsonArray("uv");
                if (uv == null || uv.size() < 2) continue;
                float[] defaultSize = bedrockFaceSize(entry.getKey(), size);
                JsonArray uvSize = face.getAsJsonArray("uv_size");
                float width = uvSize == null ? defaultSize[0] : uvSize.get(0).getAsFloat();
                float height = uvSize == null ? defaultSize[1] : uvSize.get(1).getAsFloat();
                addBedrockFace(faces, entry.getKey(), min, max,
                        uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), width, height,
                        textureWidth, textureHeight);
            }
        } else if (uvElement != null && uvElement.isJsonArray()) {
            JsonArray uv = uvElement.getAsJsonArray();
            float u = uv.get(0).getAsFloat();
            float v = uv.get(1).getAsFloat();
            float dx = Math.abs(size.x);
            float dy = Math.abs(size.y);
            float dz = Math.abs(size.z);
            addBedrockFace(faces, "west", min, max, u, v + dz, dz, dy,
                    textureWidth, textureHeight);
            addBedrockFace(faces, "north", min, max, u + dz, v + dz, dx, dy,
                    textureWidth, textureHeight);
            addBedrockFace(faces, "east", min, max, u + dz + dx, v + dz, dz, dy,
                    textureWidth, textureHeight);
            addBedrockFace(faces, "south", min, max, u + dz * 2.0F + dx, v + dz, dx, dy,
                    textureWidth, textureHeight);
            addBedrockFace(faces, "up", min, max, u + dz, v, dx, dz,
                    textureWidth, textureHeight);
            addBedrockFace(faces, "down", min, max, u + dz + dx, v + dz, dx, -dz,
                    textureWidth, textureHeight);
        }

        return new ElementDef(id, pivot, rotation, false, List.copyOf(faces));
    }

    private static float[] bedrockFaceSize(String direction, Vec size) {
        return switch (direction) {
            case "up", "down" -> new float[]{Math.abs(size.x), Math.abs(size.z)};
            case "east", "west" -> new float[]{Math.abs(size.z), Math.abs(size.y)};
            default -> new float[]{Math.abs(size.x), Math.abs(size.y)};
        };
    }

    private static void addBedrockFace(List<FaceDef> faces, String direction, Vec min, Vec max,
                                       float u, float v, float width, float height,
                                       float textureWidth, float textureHeight) {
        faces.add(new FaceDef(0, cubeFace(direction, min, max,
                u / textureWidth, v / textureHeight,
                (u + width) / textureWidth, (v + height) / textureHeight)));
    }

    private static List<TextureSize> readTextures(JsonObject root, float fallbackWidth, float fallbackHeight) {
        List<TextureSize> textures = new ArrayList<>();
        if (!root.has("textures")) return textures;
        for (JsonElement raw : root.getAsJsonArray("textures")) {
            JsonObject texture = raw.getAsJsonObject();
            textures.add(new TextureSize(
                    number(texture, "uv_width", fallbackWidth),
                    number(texture, "uv_height", fallbackHeight)));
        }
        return textures;
    }

    private static ElementDef readElement(JsonObject element, List<TextureSize> textures,
                                          float defaultWidth, float defaultHeight,
                                          boolean applyInflate) {
        String uuid = element.get("uuid").getAsString();
        Vec origin = vector(element, "origin");
        Vec rotation = vector(element, "rotation");
        boolean localMeshVertices = "mesh".equals(string(element, "type", "cube"));
        List<FaceDef> faces;
        if (localMeshVertices) {
            faces = readMeshFaces(element, textures, defaultWidth, defaultHeight);
        } else {
            faces = readCubeFaces(element, textures, defaultWidth, defaultHeight, applyInflate);
        }
        return new ElementDef(uuid, origin, rotation,
                localMeshVertices, List.copyOf(faces));
    }

    private static List<FaceDef> readMeshFaces(JsonObject element, List<TextureSize> textures,
                                               float defaultWidth, float defaultHeight) {
        Map<String, Vec> vertices = new HashMap<>();
        for (var entry : element.getAsJsonObject("vertices").entrySet()) {
            vertices.put(entry.getKey(), vector(entry.getValue().getAsJsonArray()));
        }
        List<FaceDef> result = new ArrayList<>();
        for (var entry : element.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            int texture = integer(face, "texture", 0);
            TextureSize size = textureSize(textures, texture, defaultWidth, defaultHeight);
            JsonObject uv = face.getAsJsonObject("uv");
            List<VertexDef> faceVertices = new ArrayList<>();
            for (JsonElement rawId : face.getAsJsonArray("vertices")) {
                String id = rawId.getAsString();
                JsonArray rawUv = uv.getAsJsonArray(id);
                faceVertices.add(new VertexDef(vertices.get(id), rawUv.get(0).getAsFloat() / size.width,
                        rawUv.get(1).getAsFloat() / size.height));
            }
            result.add(new FaceDef(texture, List.copyOf(faceVertices)));
        }
        return result;
    }

    private static List<FaceDef> readCubeFaces(JsonObject element, List<TextureSize> textures,
                                               float defaultWidth, float defaultHeight,
                                               boolean applyInflate) {
        Vec min = vector(element.getAsJsonArray("from"));
        Vec max = vector(element.getAsJsonArray("to"));
        if (applyInflate) {
            float inflate = number(element, "inflate", 0.0F);
            min = new Vec(min.x - inflate, min.y - inflate, min.z - inflate);
            max = new Vec(max.x + inflate, max.y + inflate, max.z + inflate);
        }
        List<FaceDef> result = new ArrayList<>();
        JsonObject faces = element.getAsJsonObject("faces");
        for (var entry : faces.entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            if (!face.has("uv")) continue;
            JsonArray uv = face.getAsJsonArray("uv");
            int texture = integer(face, "texture", 0);
            TextureSize size = textureSize(textures, texture, defaultWidth, defaultHeight);
            float u1 = uv.get(0).getAsFloat() / size.width;
            float v1 = uv.get(1).getAsFloat() / size.height;
            float u2 = uv.get(2).getAsFloat() / size.width;
            float v2 = uv.get(3).getAsFloat() / size.height;
            List<VertexDef> vertices = cubeFace(entry.getKey(), min, max, u1, v1, u2, v2);
            int quarterTurns = integer(face, "rotation", 0) / 90;
            if (quarterTurns != 0) vertices = rotateUvs(vertices, quarterTurns);
            result.add(new FaceDef(texture, vertices));
        }
        return result;
    }

    private static List<VertexDef> cubeFace(String direction, Vec a, Vec b,
                                            float u1, float v1, float u2, float v2) {
        float x0 = a.x, y0 = a.y, z0 = a.z;
        float x1 = b.x, y1 = b.y, z1 = b.z;
        return switch (direction) {
            case "east" -> vertices(
                    v(x1, y1, z0, u2, v1), v(x1, y1, z1, u1, v1),
                    v(x1, y0, z1, u1, v2), v(x1, y0, z0, u2, v2));
            case "west" -> vertices(
                    v(x0, y0, z1, u2, v2), v(x0, y1, z1, u2, v1),
                    v(x0, y1, z0, u1, v1), v(x0, y0, z0, u1, v2));
            case "up" -> vertices(
                    v(x0, y1, z1, u1, v2), v(x1, y1, z1, u2, v2),
                    v(x1, y1, z0, u2, v1), v(x0, y1, z0, u1, v1));
            case "down" -> vertices(
                    v(x1, y0, z0, u2, v2), v(x1, y0, z1, u2, v1),
                    v(x0, y0, z1, u1, v1), v(x0, y0, z0, u1, v2));
            case "south" -> vertices(
                    v(x1, y0, z1, u2, v2), v(x1, y1, z1, u2, v1),
                    v(x0, y1, z1, u1, v1), v(x0, y0, z1, u1, v2));
            default -> vertices(
                    v(x0, y1, z0, u2, v1), v(x1, y1, z0, u1, v1),
                    v(x1, y0, z0, u1, v2), v(x0, y0, z0, u2, v2));
        };
    }

    private static List<VertexDef> rotateUvs(List<VertexDef> source, int quarterTurns) {
        int shift = Math.floorMod(quarterTurns, 4);
        if (shift == 0) return source;
        List<VertexDef> rotated = new ArrayList<>(4);
        for (int i = 0; i < source.size(); i++) {
            VertexDef position = source.get(i);
            VertexDef uv = source.get(Math.floorMod(i - shift, source.size()));
            rotated.add(new VertexDef(position.position, uv.u, uv.v));
        }
        return List.copyOf(rotated);
    }

    private static GroupDef readGroup(JsonObject node, Map<String, GroupMeta> groups,
                                      Map<String, ElementDef> elements) {
        String uuid = node.get("uuid").getAsString();
        GroupMeta meta = groups.getOrDefault(uuid, new GroupMeta(uuid, Vec.ZERO, Vec.ZERO));
        List<ElementDef> directElements = new ArrayList<>();
        List<GroupDef> children = new ArrayList<>();
        if (node.has("children")) {
            for (JsonElement child : node.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    children.add(readGroup(child.getAsJsonObject(), groups, elements));
                } else {
                    ElementDef element = elements.get(child.getAsString());
                    if (element != null) directElements.add(element);
                }
            }
        }
        return new GroupDef(meta.name, meta.origin, meta.rotation,
                List.copyOf(directElements), List.copyOf(children));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                       GroupSelection selection, HeadMotion headMotion) {
        render(poseStack, consumer, light, overlay, selection, headMotion,
                GroupMotion.NONE, Map.of(), 255, 255, 255, 255, -1);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                       GroupSelection selection, HeadMotion headMotion, GroupMotion groupMotion) {
        render(poseStack, consumer, light, overlay, selection, headMotion,
                groupMotion, Map.of(), 255, 255, 255, 255, -1);
    }

    /** Renders multiple Blockbench groups with independent keyframed transforms. */
    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                       GroupSelection selection, HeadMotion headMotion,
                       Map<String, GroupTransform> groupTransforms) {
        render(poseStack, consumer, light, overlay, selection, headMotion,
                GroupMotion.NONE, groupTransforms, 255, 255, 255, 255, -1);
    }

    /**
     * Renders only faces assigned to one Blockbench texture. This lets a
     * project keep a reference entity on texture 0 while texture 1 contains
     * the actual wearable overlay, without drawing the reference twice.
     */
    public void renderTexture(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                              GroupSelection selection, HeadMotion headMotion,
                              Map<String, GroupTransform> groupTransforms, int textureIndex) {
        render(poseStack, consumer, light, overlay, selection, headMotion,
                GroupMotion.NONE, groupTransforms, 255, 255, 255, 255, textureIndex);
    }

    /**
     * The supplied flight project places both its helmet and aircraft in one
     * generic root. Render the helmet UUID separately so it can inherit the
     * cat's live head bone while the remaining aircraft follows the body.
     */
    public void renderFlightTexture(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                                    HeadMotion headMotion, Map<String, GroupTransform> groupTransforms,
                                    boolean headPart) {
        renderFiltered(poseStack, consumer, light, overlay, headMotion, groupTransforms,
                FLIGHT_HEAD_SHELL, headPart);
    }

    private void renderFiltered(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                                HeadMotion headMotion, Map<String, GroupTransform> groupTransforms,
                                String exactUuid, boolean includeExact) {
        for (GroupDef root : roots) {
            renderGroupFiltered(root, Vec.ROOT_PIVOT, poseStack, consumer, light, overlay,
                    headMotion, groupTransforms, exactUuid, includeExact);
        }
    }

    private static void renderGroupFiltered(GroupDef group, Vec parentOrigin, PoseStack poseStack,
                                            VertexConsumer consumer, int light, int overlay,
                                            HeadMotion headMotion,
                                            Map<String, GroupTransform> groupTransforms,
                                            String exactUuid, boolean includeExact) {
        poseStack.pushPose();
        GroupTransform animation = groupTransforms.getOrDefault(group.name, GroupTransform.IDENTITY);
        poseStack.translate((group.origin.x - parentOrigin.x + animation.x) / 16.0F,
                (parentOrigin.y - group.origin.y - animation.y) / 16.0F,
                (group.origin.z - parentOrigin.z + animation.z) / 16.0F);
        float x = radians(-group.rotation.x);
        float y = radians(group.rotation.y);
        float z = radians(-group.rotation.z);
        if ("head".equals(group.name)) {
            x += headMotion.xRot;
            y += headMotion.yRot;
            z += headMotion.zRot;
        }
        x += animation.xRot;
        y += animation.yRot;
        z += animation.zRot;
        rotate(poseStack, x, y, z);
        poseStack.scale(animation.scaleX, animation.scaleY, animation.scaleZ);
        for (ElementDef element : group.elements) {
            boolean exact = exactUuid != null && exactUuid.equals(element.uuid);
            if (exact != includeExact) continue;
            renderElement(element, group.origin, poseStack, consumer, light, overlay,
                    255, 255, 255, 255, OUTFIT_TEXTURE_INDEX);
        }
        for (GroupDef child : group.children) {
            renderGroupFiltered(child, group.origin, poseStack, consumer, light, overlay,
                    headMotion, groupTransforms, exactUuid, includeExact);
        }
        poseStack.popPose();
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                       GroupSelection selection, HeadMotion headMotion,
                       int red, int green, int blue, int alpha) {
        render(poseStack, consumer, light, overlay, selection, headMotion,
                GroupMotion.NONE, Map.of(), red, green, blue, alpha, -1);
    }

    private void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                        GroupSelection selection, HeadMotion headMotion, GroupMotion groupMotion,
                        Map<String, GroupTransform> groupTransforms,
                        int red, int green, int blue, int alpha, int textureFilter) {
        for (GroupDef root : roots) {
            boolean chestGroup = "chest".equals(root.name)
                    || ("group".equals(root.name) && root == roots.get(roots.size() - 1));
            if (selection == GroupSelection.CHEST_ONLY && !chestGroup) continue;
            if (selection == GroupSelection.ALL && chestGroup) continue;
            if (selection == GroupSelection.FRONT_BODY_ONLY && !"body".equals(root.name)) continue;
            if (selection == GroupSelection.CAT_HEAD_ONLY
                    && !"head".equals(root.name) && !"group".equals(root.name)) continue;
            if (selection == GroupSelection.CAT_BODY_ONLY
                    && ("head".equals(root.name) || "group".equals(root.name))) continue;
            if (selection == GroupSelection.CAT_HEAD_ONLY_PLAIN
                    && !"head".equals(root.name)) continue;
            if (selection == GroupSelection.CAT_BODY_WITH_GENERIC
                    && "head".equals(root.name)) continue;
            renderGroup(root, Vec.ROOT_PIVOT, poseStack, consumer, light, overlay, headMotion,
                    groupMotion, groupTransforms, selection, red, green, blue, alpha,
                    textureFilter);
        }
    }

    private static void renderGroup(GroupDef group, Vec parentOrigin, PoseStack poseStack,
                                    VertexConsumer consumer, int light, int overlay, HeadMotion headMotion,
                                    GroupMotion groupMotion, Map<String, GroupTransform> groupTransforms,
                                    GroupSelection selection,
                                    int red, int green, int blue, int alpha,
                                    int textureFilter) {
        poseStack.pushPose();
        GroupTransform animation = groupTransforms.getOrDefault(group.name, GroupTransform.IDENTITY);
        if (animation == GroupTransform.HIDDEN) {
            poseStack.popPose();
            return;
        }
        poseStack.translate((group.origin.x - parentOrigin.x + animation.x) / 16.0F,
                (parentOrigin.y - group.origin.y - animation.y) / 16.0F,
                (group.origin.z - parentOrigin.z + animation.z) / 16.0F);
        float x = radians(-group.rotation.x);
        float y = radians(group.rotation.y);
        float z = radians(-group.rotation.z);
        if ("head".equals(group.name)) {
            x += headMotion.xRot;
            y += headMotion.yRot;
            z += headMotion.zRot;
        }
        if (group.name.equals(groupMotion.name)) {
            x += groupMotion.xRot;
            y += groupMotion.yRot;
            z += groupMotion.zRot;
        }
        x += animation.xRot;
        y += animation.yRot;
        z += animation.zRot;
        rotate(poseStack, x, y, z);
        poseStack.scale(animation.scaleX, animation.scaleY, animation.scaleZ);
        ElementDef frontBody = selection == GroupSelection.FRONT_BODY_ONLY
                ? group.elements.stream().max((a, b) -> Float.compare(a.origin.y, b.origin.y)).orElse(null)
                : null;
        for (ElementDef element : group.elements) {
            if (selection == GroupSelection.FRONT_BODY_ONLY && element != frontBody) continue;
            renderElement(element, group.origin, poseStack, consumer, light, overlay,
                    red, green, blue, alpha, textureFilter);
        }
        for (GroupDef child : group.children) {
            renderGroup(child, group.origin, poseStack, consumer, light, overlay, headMotion,
                    groupMotion, groupTransforms, selection, red, green, blue, alpha,
                    textureFilter);
        }
        poseStack.popPose();
    }

    private static void renderElement(ElementDef element, Vec parentOrigin, PoseStack poseStack,
                                      VertexConsumer consumer, int light, int overlay,
                                      int red, int green, int blue, int alpha,
                                      int textureFilter) {
        poseStack.pushPose();
        poseStack.translate((element.origin.x - parentOrigin.x) / 16.0F,
                (parentOrigin.y - element.origin.y) / 16.0F,
                (element.origin.z - parentOrigin.z) / 16.0F);
        float x = radians(-element.rotation.x);
        float y = radians(element.rotation.y);
        float z = radians(-element.rotation.z);
        if (FISHING_RIGHT_FISH.equals(element.uuid)) {
            rotateBlockbenchMeshXyz(poseStack, x, y, z);
        } else {
            rotate(poseStack, x, y, z);
        }
        for (FaceDef face : element.faces) {
            if (textureFilter >= 0 && face.textureIndex != textureFilter) continue;
            renderFace(face, element.origin, element.localMeshVertices,
                    poseStack, consumer, light, overlay, red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private static void renderFace(FaceDef face, Vec origin, boolean localVertices, PoseStack poseStack,
                                   VertexConsumer consumer, int light, int overlay,
                                   int red, int green, int blue, int alpha) {
        if (face.vertices.size() < 3) return;
        List<LocalVertex> vertices = new ArrayList<>(face.vertices.size());
        for (VertexDef vertex : face.vertices) {
            vertices.add(new LocalVertex(
                    (localVertices ? vertex.position.x : vertex.position.x - origin.x) / 16.0F,
                    (localVertices ? -vertex.position.y : origin.y - vertex.position.y) / 16.0F,
                    (localVertices ? vertex.position.z : vertex.position.z - origin.z) / 16.0F,
                    vertex.u, vertex.v));
        }

        LocalVertex p0 = vertices.get(0);
        LocalVertex p1 = vertices.get(1);
        LocalVertex p2 = vertices.get(2);
        Vector3f edge1 = new Vector3f(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
        Vector3f edge2 = new Vector3f(p2.x - p0.x, p2.y - p0.y, p2.z - p0.z);
        Vector3f normal = edge1.cross(edge2, new Vector3f()).negate();
        if (normal.lengthSquared() < 1.0E-8F) return;
        normal.normalize();

        PoseStack.Pose pose = poseStack.last();
        for (LocalVertex vertex : vertices) {
            consumer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                    .setColor(red, green, blue, alpha)
                    .setUv(vertex.u, vertex.v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    private static void rotate(PoseStack poseStack, float x, float y, float z) {
        if (x == 0.0F && y == 0.0F && z == 0.0F) return;
        poseStack.mulPose(new Quaternionf().rotationZYX(z, y, x));
    }

    /** Blockbench/Three.js Euler order XYZ (quaternion qx * qy * qz). */
    private static void rotateBlockbenchMeshXyz(PoseStack poseStack, float x, float y, float z) {
        if (x == 0.0F && y == 0.0F && z == 0.0F) return;
        poseStack.mulPose(new Quaternionf().rotationXYZ(x, y, z));
    }

    private static TextureSize textureSize(List<TextureSize> textures, int index,
                                           float fallbackWidth, float fallbackHeight) {
        if (index >= 0 && index < textures.size()) return textures.get(index);
        return new TextureSize(fallbackWidth, fallbackHeight);
    }

    private static Vec vector(JsonObject object, String key) {
        return object.has(key) ? vector(object.getAsJsonArray(key)) : Vec.ZERO;
    }

    private static Vec vector(JsonArray array) {
        return new Vec(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static float number(JsonObject object, String key, float fallback) {
        return object.has(key) ? object.get(key).getAsFloat() : fallback;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }

    private static VertexDef v(float x, float y, float z, float u, float v) {
        return new VertexDef(new Vec(x, y, z), u, v);
    }

    private static List<VertexDef> vertices(VertexDef a, VertexDef b, VertexDef c, VertexDef d) {
        return List.of(a, b, c, d);
    }

    public enum GroupSelection {
        /** Historical behaviour: excludes a final generic chest group. */
        ALL,
        /** Includes every authored root group, including a generic accessory group. */
        ALL_GROUPS,
        CHEST_ONLY,
        FRONT_BODY_ONLY,
        /** Cat head plus the clothing project's generic head-attached accessory. */
        CAT_HEAD_ONLY,
        /** All cat roots except the head and its generic attached accessory. */
        CAT_BODY_ONLY,
        /** Only the named vanilla head root; generic roots are body accessories. */
        CAT_HEAD_ONLY_PLAIN,
        /** All roots except the head, including a generic body/back accessory root. */
        CAT_BODY_WITH_GENERIC
    }

    public record HeadMotion(float xRot, float yRot, float zRot) {
        public static final HeadMotion NONE = new HeadMotion(0.0F, 0.0F, 0.0F);
    }

    public record GroupMotion(String name, float xRot, float yRot, float zRot) {
        public static final GroupMotion NONE = new GroupMotion("", 0.0F, 0.0F, 0.0F);
    }

    /** Position is in Blockbench pixels; rotation is in radians; scale is local to the group pivot. */
    public record GroupTransform(float x, float y, float z, float xRot, float yRot, float zRot,
                                 float scaleX, float scaleY, float scaleZ) {
        public GroupTransform(float x, float y, float z, float xRot, float yRot, float zRot) {
            this(x, y, z, xRot, yRot, zRot, 1.0F, 1.0F, 1.0F);
        }

        public static final GroupTransform IDENTITY =
                new GroupTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        public static final GroupTransform HIDDEN =
                new GroupTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        public static GroupTransform position(float x, float y, float z) {
            return new GroupTransform(x, y, z, 0.0F, 0.0F, 0.0F);
        }

        public static GroupTransform rotation(float xRot, float yRot, float zRot) {
            return new GroupTransform(0.0F, 0.0F, 0.0F, xRot, yRot, zRot);
        }

        public static GroupTransform scaled(float x, float y, float z,
                                            float xRot, float yRot, float zRot,
                                            float scaleX, float scaleY, float scaleZ) {
            return new GroupTransform(x, y, z, xRot, yRot, zRot, scaleX, scaleY, scaleZ);
        }
    }

    private record TextureSize(float width, float height) {}
    private record Vec(float x, float y, float z) {
        private static final Vec ZERO = new Vec(0.0F, 0.0F, 0.0F);
        private static final Vec ROOT_PIVOT = new Vec(0.0F, 24.0F, 0.0F);
    }
    private record VertexDef(Vec position, float u, float v) {}
    private record LocalVertex(float x, float y, float z, float u, float v) {}
    private record FaceDef(int textureIndex, List<VertexDef> vertices) {}
    private record ElementDef(String uuid, Vec origin, Vec rotation, boolean localMeshVertices,
                              List<FaceDef> faces) {}
    private record GroupMeta(String name, Vec origin, Vec rotation) {}
    private record GroupDef(String name, Vec origin, Vec rotation, List<ElementDef> elements,
                            List<GroupDef> children) {}
    private enum CropEdge { MINIMUM, MAXIMUM }
    private record BedrockBone(String name, String parent, Vec pivot, Vec rotation,
                               List<ElementDef> cubes) {}

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }

    private RuntimeBlockbenchModel() {
        this(List.of());
    }
}
