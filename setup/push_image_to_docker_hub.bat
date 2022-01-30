docker build -t swc-setup-mongo .
docker tag swc-setup-mongo:latest stepanov1997/swc-setup-mongo:latest
docker push stepanov1997/swc-setup-mongo:latest
