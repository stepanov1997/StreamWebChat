rsconf = {
    _id: "rs0",
    members: [
        {_id: 0, host: "mongo-rs0-1:27017"},
        {_id: 1, host: "mongo-rs0-2:27018"},
        {_id: 2, host: "mongo-rs0-3:27019"}
    ]
}

rs.initiate(rsconf);

rs.conf();
