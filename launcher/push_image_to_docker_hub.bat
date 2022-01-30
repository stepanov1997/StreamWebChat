docker build -t swc-launcher -f Dockerfile-runner .
docker tag swc-launcher:latest stepanov1997/swc-launcher:latest
docker push stepanov1997/swc-launcher:latest
