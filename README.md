# Receive

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

> Work in progress

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
