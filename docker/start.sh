#!/bin/bash
echo "Starting REST interface."
echo "Arguments: $1"

# updating
cd /etc/headless/PCM-Headless/org.pcm.headless.rest/
git pull
gradle bootJar

# build agent
cd /etc/headless/PCM-Headless/org.pcm.headless.agent/
gradle clean build jar

# running
cd /etc/headless/PCM-Headless/org.pcm.headless.rest/build/libs/

# var
jarFile="$(find . -name "*.jar" -maxdepth 1 -type f)"

java -javaagent:/etc/headless/PCM-Headless/org.pcm.headless.agent/build/libs/agent.jar -jar $jarFile $1