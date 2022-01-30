$debugExists=0
$keepPodsAlive=0

# Counting a parameters
foreach ($param in $args)
{
    if ($param -eq "-debug")
    {
        $debugExists=$debugExists+1
    }
    if ( $param -eq "-keepPodsAlive")
    {
        $keepPodsAlive=$keepPodsAlive+1
    }
}

kubectl.exe delete --ignore-not-found=true service swc-runner
kubectl.exe delete --ignore-not-found=true deployment swc-runner-debug
kubectl.exe delete --ignore-not-found=true job swc-runner-job

# Keep pods alive on start tests
if ($keepPodsAlive -eq 0 )
{
    # Cleaning environment if something exists
    kubectl delete --ignore-not-found=true namespace "swc-namespace"
}

# Directories initialisation
$scriptpath = $MyInvocation.MyCommand.Path
$dir = Split-Path $scriptpath
$ROOT_PATH = (get-item $dir ).parent.parent.FullName

# Maven clean install
Set-Location $ROOT_PATH\kubernetes-runner-library\
mvn clean install
Set-Location $ROOT_PATH\launcher\
mvn clean install

# Docker build, tag and push on registry
if ($debugExists -gt 0 )
{
    $Dockerfile = "Dockerfile-runner-debug"
}
else
{
    $Dockerfile = "Dockerfile-runner"
}
docker build -t swc-runner:latest -f ${Dockerfile} .
docker tag swc-runner:latest localhost:5000/swc-runner:latest
docker push localhost:5000/swc-runner:latest

# Applying docker image on Kubernetes cluster
if ($debugExists -gt 0 )
{
    kubectl apply -f "local/runner-debug.yaml"
}
else
{
    kubectl apply -f "local/runner-job.yaml"
}
Set-Location $dir
