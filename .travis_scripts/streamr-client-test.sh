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

echo "Create fatJar"
./gradlew fatjar

echo "Get filename to sed in"
client_name=$(ls build/libs)

echo "Testing Tool setup"
git clone https://github.com/streamr-dev/streamr-client-testing.git
cd $TRAVIS_BUILD_DIR/streamr-client-testing

echo "Bring jar into easy referenced folder, no need to escape special chars."
mkdir client-jar
mv $TRAVIS_BUILD_DIR/build/libs/$client_name ./client-jar/

echo "Setup build.gradle to use local jar and latest JS client"
sed -i "s/compile 'com.streamr:client:1.3.0'/compile fileTree(dir: 'client-jar', include: '*.jar')/g" build.gradle
sed -i "s/\"streamr-client\": \"\^3.1.2\"/\"streamr-client\":\"latest\"/g" package.json

echo "Prepare for test"
## npm install is used because package-lock.json could be form a previous client version.
npm install
./gradlew fatjar

echo "Run streamr-client test"
java -jar build/libs/client_testing-1.0-SNAPSHOT.jar -s stream-encrypted-shared-rotating-signed -m test

