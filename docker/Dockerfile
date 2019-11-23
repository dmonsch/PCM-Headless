FROM openjdk:12

MAINTAINER David Monschein

ADD start.sh /start.sh 

# Basic installations
RUN \
	yum -y update &&\
	yum -y install unzip && \
	yum -y install nano && \
	yum -y install git && \
	yum -y install curl wget
	
RUN yum -y update && yum -y install dos2unix && \
	dos2unix /start.sh
RUN yum -y install bash
	
#Make the scripts executable 
RUN chmod +x /*.sh

# install gradle
ENV GRADLE_VERSION=5.6.4
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