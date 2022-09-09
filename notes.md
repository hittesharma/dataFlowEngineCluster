Compile with
$ docker run --rm -it --entrypoint /bin/bash -v "$(pwd):/code" --workdir /code maven:3-jdk-8
$ mvn package