package cn.laowu.mod.painting;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Server-world content-addressed assets. All calls belong on a bounded IO worker. */
public final class PaintingAssetStore {
    public record AssetInfo(String hash,int pixelWidth,int pixelHeight,long bytes,UUID owner) {}
    private final Path directory;
    private final long quota;
    private volatile long used;
    public PaintingAssetStore(Path worldRoot,long quotaBytes)throws IOException {
        if(quotaBytes<0)throw new IllegalArgumentException("quota");
        quota=quotaBytes;directory=worldRoot.toAbsolutePath().normalize().resolve("laowu/cat_paintings");
        Files.createDirectories(directory);
        if(Files.isSymbolicLink(directory) || Files.isSymbolicLink(directory.getParent()))throw new IOException("asset_directory");
        try(var paths=Files.list(directory)){
            for(Path p:paths.toList())if(p.getFileName().toString().matches("[0-9a-f]{64}\\.png") && Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))used=Math.addExact(used,Files.size(p));
        }
    }
    public static String hash(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static boolean validHash(String hash){return hash!=null && hash.matches("[0-9a-f]{64}");}
    private Path path(String hash)throws IOException{
        if(!validHash(hash))throw new IOException("asset_hash");
        Path p=directory.resolve(hash+".png");
        if(Files.isSymbolicLink(p))throw new IOException("asset_path");
        return p;
    }
    public synchronized AssetInfo put(byte[] png,PaintingSpec spec,UUID owner)throws IOException{
        byte[] canonical=PaintingImages.validateUpload(png,spec);
        String hash=hash(canonical);Path target=path(hash);
        if(Files.exists(target)){
            if(!Arrays.equals(read(hash),canonical))throw new IOException("asset_corrupt");
        }else{
            if(canonical.length>quota-used)throw new IOException("asset_quota");
            Path temp=Files.createTempFile(directory,"upload-",".tmp");
            try{
                Files.write(temp,canonical,StandardOpenOption.TRUNCATE_EXISTING);
                try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,target);}
                used+=canonical.length;
            }finally{Files.deleteIfExists(temp);}
        }
        Path attribution=directory.resolve(hash+".owner");
        if(Files.isSymbolicLink(attribution))throw new IOException("asset_owner_path");
        if(!Files.exists(attribution)){
            Path temp=Files.createTempFile(directory,"owner-",".tmp");
            try{
                Files.writeString(temp,owner.toString(),StandardOpenOption.TRUNCATE_EXISTING);
                try{Files.move(temp,attribution,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,attribution);}
            }finally{Files.deleteIfExists(temp);}
        }
        if(Files.size(attribution)>80)throw new IOException("asset_owner");
        UUID firstOwner;
        try{firstOwner=UUID.fromString(Files.readString(attribution).trim());}catch(IllegalArgumentException e){throw new IOException("asset_owner",e);}
        var image=ImageIO.read(new ByteArrayInputStream(canonical));
        try{return new AssetInfo(hash,image.getWidth(),image.getHeight(),canonical.length,firstOwner);}
        finally{image.flush();}
    }
    public synchronized byte[] read(String hash)throws IOException{
        Path p=path(hash);long length=Files.size(p);
        if(length<1 || length>PaintingImages.MAX_PNG_BYTES)throw new IOException("asset_size");
        byte[] data;
        try(var in=Files.newInputStream(p)){data=in.readNBytes(PaintingImages.MAX_PNG_BYTES+1);}
        if(data.length!=length || !hash(data).equals(hash))throw new IOException("asset_corrupt");
        return data;
    }
    public long usedBytes(){return used;}
}
