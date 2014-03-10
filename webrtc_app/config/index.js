
var wsuri, uri;

var ip;

ip = "192.168.1.10";
//ip = "192.168.9.218";

var port = 3001;

var serverport = port-1;
var serverip = ip;

wsuri = 'wss://'  + ip + ':' + port ;
uri = 'https://'  + ip + ':' + port ;

var config = {
    room: 'robopet',
    uri: uri,
    wsuri: wsuri
};


exports.config = config;
exports.ip = ip;
exports.port = port;

exports.serverport = serverport;
exports.serverip = serverip;
