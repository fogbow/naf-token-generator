# Naf Token Generator 

## Install

To get the lastest stable version of the NaF token generator, download it from our repository:

```
wget https://github.com/fogbow/naf-token-generator/archive/master.zip
```

Then, decompress it:
```
unzip master.zip
```

Now, install it with Maven:
```
cd naf-token-generator-master
mvn install
```

## Configure

After the installation, rename the **tokengenerator.conf.example** file to **tokengenerator.conf** and edit its contents. The **tokengenerator.conf.example** file describes both mandatory and optional properties. Below we show some of the mandatory properties:

````
http_port=$http_port

# The Token Generator needs a keypair of the Fogbow site admin to sign and verify tokens
admin_public_key=$path-public-key
admin_private_key=$path-private-key

# Authentication plugin
authentication_plugin=org.fogbowcloud.generator.auth.LDAPAuthentication
# ldap service url
ldap_url=$lad_url
# LDAP base credentials to be used when interacting with the LDAP server
# example: dc=lab,dc=inst,dc=edu,dc=br
ldap_base=$ldap_base

# comma separated list of Token generator admin usernames 
# (these users are allowed to perform special tasks, such as deleting tokens)
admins=$username1,$username2

# path to token database (will be created if does not exist)
token_datastore_url=jdbc:sqlite:$path/db_token_SQLite.db
```

## Token Generator Server

To start the Token Generator server, run the **start-token-generator** script inside ```bin/``` directory.

```bash
bin/start-token-generator
```

## Token Generator CLI
After install the **Token Generator**, the **Token Generetor CLI** script can be found in ```bin/```directory.

### Create Token
To create a token, run the CLI with the **POST** command:
```bash
token=`bin/token-generator-cli POST -n name [-h hours or -i]`
Password: ***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
  -h or -hours Number of hours that the token will remain valid. (Optional)
  or
  -i Reequest a token without expiration time (infinite). (Optional)
```

### Get tokens (Requires admin role) 
To retrieve tokens, run the CLI t with the **GET** command:
```bash
bash bin/token-generator-cli GET -n name
Password: ***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
```

### Check the validity of a specific token
To check token validity, run the CLI with the **GET** command:
```bash
bash bin/token-generator-cli GET -n name -t token
Password:***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
  -t or -token Token (Required)
```

### Remove specific token (Requires admin role) 
To remove a specific token, run the CLI with the **DELETE** command:
```bash
bash bin/token-generator-cli DELETE -n name -t token
Password: ***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
  -t or -token Token (Required)
```
