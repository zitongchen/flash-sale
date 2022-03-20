FROM kdvolder/jdk8

# 在镜像中创建一个卷，在容器启动的时候会在 xx/docker 目录下面创建一个属于当前容器的 /tmp 目录，spring boot 内置 tomcat 的内容会输出到 /tmp 目录中
VOLUME /tmp
ADD ./start/target/start-0.0.1-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app.jar'

# 在 docker run 的时候可以修改 JAVA_OPTS 的值，在 docker-compose 定义中可以修改 JAVA_OPTS 的值
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]