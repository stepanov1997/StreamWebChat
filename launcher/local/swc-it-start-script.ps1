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

kubectl.exe delete --ignore-not-found=true service integration-tests
kubectl.exe delete --ignore-not-found=true deployment integration-tests
kubectl.exe delete --ignore-not-found=true job integration-tests

# Keep pods alive on start tests
if ($keepPodsAlive -eq 0 )
{
    # Cleaning environment if something exists
    kubectl delete namespace "integration-tests-namespace"
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
    $Dockerfile = "Dockerfile-it-runner-debug"
}
else
{
    $Dockerfile = "Dockerfile-it-runner"
}
docker build -t integration-tests:latest -f ${Dockerfile} .
docker tag integration-tests:latest localhost:5000/integration-tests:latest
docker push localhost:5000/integration-tests:latest

# Applying docker image on Kubernetes cluster
if ($debugExists -gt 0 )
{
    kubectl apply -f "local/integration-tests-debug.yaml"
}
else
{
    kubectl apply -f "local/integration-tests-job.yaml"
}
Set-Location $dir
