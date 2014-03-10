RoboPet webrtc app
===

node.js based application used to control and communicate with the robopet Android app.

Installation
---
To setup the application run 

`npm install`

`bower install`

For testing purposes, the SSL certificates (required to have webrtc video on the browser) used are self-signed. The android application uses unencrypted connection.
Set-up stunnel to communicate both over SSL and without. On ubuntu/debian-like

`sudo apt-get stunnel`

In /etc/default/stunnel set to 1 the enabled flag

`sudo cp ./stunnel.conf /etc/stunnel/webrtc.conf` 

`sudo service stunnel restart`

Configuration
---

Launch the app
---

`node app.js`

Point the browser to https://<IP:PORT>/ to get the control ui and on another browser https://<IP:PORT>/pet for the pet ui.
In the Android app, update your ip accoridingly in the source (@TODO: add a textbox for this..)

Credits
---

Many thanks to 
* @HenrikJoreteg for SimpleWebRTC, other libs and inspiration (https://github.com/HenrikJoreteg/SimpleWebRTC)
* @jairajs89 for touchy.js (https://github.com/jairajs89/Touchy.js)

License 
---

MIT
