# timestill-maven-plugin
Maven plugin to freeze timestamp of files inside a jar or war file. 

When using docker it is undesirable for checksums to change of jar and war files when just making a new build of unchanged code. For docker these changed checksum indicates that a layer is changed and a new layer will be created for you at the expense of build time and storage space.

When a project creates a single docker image it might not be a problem, but having a multi module project creating multiple docker images this becomes a problem, because on every build all docker images are build.
