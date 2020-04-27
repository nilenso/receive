# Receive

[![pipeline status](https://gitlab.com/nilenso/receive/badges/master/pipeline.svg)](https://gitlab.com/nilenso/receive/-/commits/master)

A one click install application for file sharing.

Setup a full stack file sharing application on the platform of your choice.

> Note: Project is still in development and all the features are not ready yet.

## Getting started

### Prerequisites

You'll need the following softwares to get started with development.

- Clojure
- Leiningen
- Postgres

### Installing

- `mkdir <path to file upload>`
- update config.edn in resources with <path to file upload>
- `lein deps`
- `cp resources/database.sample.edn resources/database.edn` and replace config
- `lein migrate` to setup database

### Running

Run `start-dev-server` in `core.clj` to start an auto reload development server

## Testing

`lein test`

## Deployment

### Requirements

- Java
- Clojure
- Leiningen
- Postgres
  
### Setup

Add systemd services for both staging and production environement using the following script and place it in `/etc/systemd/system` and name the file `receive.staging.service`

```
[Unit]
Description=Receive API

[Service]
Environment=NOMAD_INSTANCE=PROD
Environment=PORT=3000
Type=simple
ExecStart=/usr/bin/lein run
Restart=always
User=root
WorkingDirectory=/opt/staging/receive

[Install]
WantedBy=mutli-user.target
```

Run the following commands to start the service and enable auto restart

```
sudo systemctl start receive.staging.service
sudo systemctl enable receive.staging.service
```

For nginx add the following server block to forward the request to the service

```
server {
	listen 80;
	listen [::]:80;

	server_name receive-staging.nilenso.com;

	location / {
		proxy_pass http://localhost:3000;
	}
}
```

Save the database config in `/opt/database.staging.edn`. Sample config can be found in `resources/database.sample.edn`

Repeat the same processs for the production setup.

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
