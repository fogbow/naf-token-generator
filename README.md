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

## Token Generator CLI
After downloading a **Token Generator**, the **Token Generetor CLI** script can be found in ```bin/```directory.

### Create Token
To create a token, run the token generator script with the **POST** command:
```bash
token=`bin/token-generator-cli POST -n name [-h hours or -i]`
Password: ***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
  -h or -hours Number of hours that the token will be valid. (Optional)
  or
  -i Use to create token infinite, without expiration time. (Optional)
```

### Get tokens (Admin operation) 
To retrieve tokens, run the arrebol script with the **GET** command:
```bash
bash bin/token-generator-cli GET -n name
Password: ***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
```

### Check the validity of a specific token
To check token validity, run the arrebol script with the **GET** command:
```bash
bash bin/token-generator-cli GET -n name -t token
Password:***
```
Summary:
```shell
  -n or -name Username for authentication (Required)
  -t or -token Token (Required)
```

### Remove specific token (Admin operation) 
To remove a specific token, run the arrebol script with the **DELETE** command:
```bash
bash bin/token-generator-cli DELETE -n name -t token
Password: ***
```
Summary;
```shell
  -n or -name Username for authentication (Required)
  -t or -token Token (Required)
```
