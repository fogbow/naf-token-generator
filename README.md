# Naf Token Generator 

## Token Generator CLI
After unpacking a **Token Generator** release package (listed [here](...)), the **Token Generetor CLI** script can be found in ```bin/```directory.

### Create Token

To create a token, run the token generator script with the **POST** command:

```shell
  -n or -name _ Username for authentication (Required)
  ** important ** The password will be required after command.
  
  -h or -hours _ Number of hours that the token will be valid. (Optional)
  or
  -i _ Use to create token infinite, without expiration time. (Optional)
```

```bash
token=`bin/token-generator-cli POST -n name -h hours or -i`
Password: **********
```

### Get tokens (Admin operation) 
To retrieve tokens, run the arrebol script with the **GET** command:

```shell
  -n or -name _ Username for authentication (Required)
  ** important ** The password will be required after command.
```

```bash
bash bin/token-generator-cli GET -n name
Password: **********
```

### Check the validity of a specific token (Admin operation) 
To check token validity, run the arrebol script with the **GET** command:

```shell
  -n or -name _ Username for authentication (Required)
  ** important ** The password will be required after command.
  
  -t or -token _ Token (Required)
```

```bash
bash bin/token-generator-cli GET -n name -t token
Password: **********
```

### Remove specific token (Admin operation) 
To remove a specific token, run the arrebol script with the **DELETE** command:

```shell
  -n or -name _ Username for authentication (Required)
  ** important ** The password will be required after command.
  
  -t or -token _ Token (Required)
```

```bash
bash bin/token-generator-cli DELETE -n name -t token
Password: **********
```
