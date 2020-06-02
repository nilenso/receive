env="staging"
if [ $1 = "production" ];
    then
        env="production"
fi

echo "\nSetting up [$env]"

echo "Cloning Repository"
git clone https://gitlab.com/nilenso/receive.git --quiet

echo "Copying files"
rm -rf /opt/$env/
mkdir /opt/$env/
mv receive /opt/$env/
cd /opt/$env/receive
cp /opt/database.$env.edn /opt/$env/receive/resources/database.edn
cp /opt/secrets.$env.edn /opt/$env/receive/resources/secrets.edn

echo "Running lein deps"
lein deps > /dev/null

echo "Running database migration"
lein migrate

echo "Restarting service"
systemctl restart receive.$env.service
echo "Setup Completed"