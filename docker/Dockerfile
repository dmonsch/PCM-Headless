FROM openjdk:12-alpine

MAINTAINER David Monschein

ADD start.sh /start.sh 

# Basic installations
RUN \
	apk update &&\
	apk add unzip && \
	apk add nano && \
	apk add git && \
	apk add curl wget
	
RUN apk update && apk add dos2unix && \
	dos2unix /start.sh
RUN apk add --no-cache --upgrade bash
	
#Make the scripts executable 
RUN chmod +x /*.sh

# install gradle
ENV GRADLE_VERSION=5.6.3
ENV GRADLE_HOME /home/user/gradle-$GRADLE_VERSION
ENV PATH $GRADLE_HOME/bin:$PATH

RUN wget -P /home/user/ --quiet https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip && \
cd /home/user/ && unzip gradle-$GRADLE_VERSION-bin.zip && rm gradle-$GRADLE_VERSION-bin.zip

# build gradle
RUN mkdir /etc/headless && \
	cd /etc/headless && \
	git clone https://github.com/dmonsch/PCM-Headless && \
	cd /etc/headless/PCM-Headless/org.pcm.headless.rest/ && \
	gradle clean bootJar
	
EXPOSE 8080 8080

ENTRYPOINT ["/start.sh"] 
CMD []