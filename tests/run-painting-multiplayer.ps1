param(
 [ValidateSet('forge','neo')][string]$Loader='forge',
 [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunId=('painting-'+(Get-Date -Format 'yyyyMMdd-HHmmss')),
 [int]$TimeoutSeconds=600
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$port=if($Loader -eq 'forge'){'forge-1.20.1'}else{'neoforge-1.21.1'}
$project=Join-Path $root $port
$base=Join-Path $project ('build/painting-multiplayer-'+$RunId)
if(Test-Path -LiteralPath $base){throw 'Use a new RunId to preserve prior test evidence'}
$jdk=if($Loader -eq 'forge'){'C:/Program Files/Zulu/zulu-17'}else{'C:/Program Files/Zulu/zulu-21'}
if(!(Test-Path -LiteralPath ($jdk+'/bin/java.exe'))){throw 'Configure the installed JDK path in this test runner'}
$listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0);$listener.Start();$tcpPort=$listener.LocalEndpoint.Port;$listener.Stop()
$children=[Collections.Generic.List[object]]::new()
$oldJava=$env:JAVA_HOME
function Launch-Probe([string]$role,[string]$label){
 $log=Join-Path $project ('build/painting-'+$RunId+'-'+$label+'.log')
 $err=Join-Path $project ('build/painting-'+$RunId+'-'+$label+'.err.log')
 $task=if($role -eq 'server'){'runServer'}else{'runClient'}
 $arguments='gradlew.bat '+$task+' --offline --init-script ../tests/painting-multiplayer.init.gradle --no-configuration-cache -PpaintingRole='+$role+' -PpaintingRunId='+$RunId+' -PpaintingPort='+$tcpPort
 $process=Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d','/c',$arguments) -WorkingDirectory $project -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
 $child=[pscustomobject]@{Role=$role;Process=$process;Log=$log;ErrorLog=$err};$children.Add($child);return $child
}
function Await-Line($child,[string]$pattern){
 $deadline=(Get-Date).AddSeconds($TimeoutSeconds)
 while((Get-Date) -lt $deadline){
  $content=if(Test-Path -LiteralPath $child.Log){Get-Content -LiteralPath $child.Log -Raw}else{''}
  if($content -match $pattern){return}
  if($content -match 'FAIL: PAINTING|BUILD FAILED'){throw ('Test failed: '+$child.Log)}
  if($child.Process.HasExited){throw ('Process exited before acceptance: '+$child.Log)}
  Start-Sleep -Seconds 2
 }
 throw ('Timed out: '+$child.Log)
}
function Await-Exit($child){
 if(!$child.Process.WaitForExit(60000)){throw ('Test process did not exit: '+$child.Log)}
}
try{
 $env:JAVA_HOME=$jdk
 Write-Output 'Starting isolated loopback server'
 $server=Launch-Probe 'server' 'server'
 Await-Line $server 'PASS: PAINTING DEDICATED SERVER READY'
 Write-Output 'Starting uploader with a native offline test username'
 $a=Launch-Probe 'a' 'a'
 Await-Line $server 'PainterA.*joined the game'
 $b=Launch-Probe 'b' 'b'
 Await-Line $a 'PASS: PAINTING CLIENT A PNG'
 Await-Line $b 'PASS: PAINTING OTHER CLIENT THREE IMAGES b'
 Await-Line $server 'PASS: PAINTING MULTIPLAYER SERVER THREE SAVED ASSETS'
 Await-Exit $a;Await-Exit $b;Await-Exit $server
 Write-Output 'Restarting the saved server, using a fresh client directory with no source or cache'
 $server2=Launch-Probe 'server' 'restart-server'
 Await-Line $server2 'PASS: PAINTING DEDICATED SERVER READY'
 $fresh=Launch-Probe 'b2' 'fresh-client'
 Await-Line $fresh 'PASS: PAINTING OTHER CLIENT THREE IMAGES b2'
 Await-Line $server2 'PASS: PAINTING MULTIPLAYER SERVER THREE SAVED ASSETS'
 Await-Exit $fresh;Await-Exit $server2
 Write-Output ('PASS: '+$Loader+' multiplayer PNG/JPG, modes/crops, other player, restart and fresh-cache recovery: '+$base)
}finally{
 $env:JAVA_HOME=$oldJava
 # Only cmd/Gradle/Java process trees launched by THIS invocation are owned here.
 foreach($child in $children){if(!$child.Process.HasExited){$child.Process.Kill($true);Write-Warning ('Stopped isolated test process tree: '+$child.Role)}}
}

