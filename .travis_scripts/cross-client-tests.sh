#!/usr/bin/env bash
set -e

sudo /etc/init.d/mysql stop
if [ ! -d streamr-docker-dev ]; then # Skip clone on subsequent attemps.
	git clone https://github.com/streamr-dev/streamr-docker-dev.git
fi

sudo ifconfig docker0 10.200.10.1/24
"$TRAVIS_BUILD_DIR/streamr-docker-dev/streamr-docker-dev/bin.sh" start smart-contracts-init nginx engine-and-editor --wait

echo "Clean out install phase build"
rm -rf build/libs

echo "Create client fatJar"
./gradlew fatjar

echo "Get client jar filename"
client_name=$(ls build/libs)

echo "Testing Tool setup"
git clone https://github.com/streamr-dev/streamr-client-testing.git
cd $TRAVIS_BUILD_DIR/streamr-client-testing

echo "Bring jar into easy referenced folder, no need to escape special chars."
mkdir client-jar
mv $TRAVIS_BUILD_DIR/build/libs/$client_name ./client-jar/

echo "Setup build.gradle to use local jar and latest published JS client"
sed -i -E "s/compile 'com.streamr:client:.*'/compile fileTree(dir: 'client-jar', include: '*.jar')/g" build.gradle
sed -i -E "s/\"streamr-client\": \".*\"/\"streamr-client\": \"latest\"/g" package.json

echo "Prepare for test"
## npm install is used because package-lock.json could be form a previous client version.
npm install
./gradlew fatjar

echo "Run cross-client test scenarios"
# TODO: change java-only-ci.conf to default-ci.conf once JS client is compatible
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -m test -s stream-cleartext-unsigned -c config/default-ci.conf && \
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -m test -s stream-cleartext-signed -c config/default-ci.conf && \
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -m test -s stream-encrypted-shared-signed -c config/java-only-ci.conf && \
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -m test -s stream-encrypted-shared-rotating-signed -c config/java-only-ci.conf && \
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -m test -s stream-encrypted-exchanged-rotating-signed -c config/java-only-ci.conf && \
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -m test -s stream-encrypted-exchanged-rotating-revoking-signed -c config/java-only-ci.conf
