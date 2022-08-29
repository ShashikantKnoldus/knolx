<h1>Knolx Service</h1>

<h3>Knolx service requires the following:</h3>
1. <a href="https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/">Mongod</a>
2. <a href="https://www.mongodb.com/try/download/compass">MongoCompass</a>
3. <a href="https://drive.google.com/drive/u/2/folders/149BHfwUYxBAdaW2QBoxVvk1aaT42tkxI">Sample Data set</a>
4. Put the file Credential.json in `/home/knoldus/` [Found in the same folder from step 3]
5. Export GOOGLEAPICREDENTIALPATH="/home/knoldus/credential.json"
<p>You can find the two mentioned files above in this link: <a href="https://drive.google.com/drive/folders/1Di-ofsLRMHdWv6Fq8vMmbfCcf0ujCXup?usp=sharing">here</a></p>



<h3>Setting up local database</h3>
<p>
After completing the steps mentioned above, you will need to set up a local database!
</p>

1. Run `sudo mongod` in terminal to start the mongodb locally.
2. Open MongoCompass > New connection > Connect to the default URI (You can also favourite this connection to reconnect to it easily!)
3. Go to Databases > Create a new Database called Knolx
4. Now you can create collections for your new database. (One collection for each file mentioned in the Sample_Data_Set)
5. You should have a valid mongod server running in your local that has a database called knolx and contains some collections.

<h3> Running knolx service locally </h3>

1. Checkout to develop from terminal.
2. Run `sbt run` from the root directory.
3. Knolx service should be running on http://localhost:8000


<h4> Hitting endpoints </h4>
To hit most endpoint, you will need a valid bearer token.<br>
To get a token, run this command in terminal <br>

1. Run the following command in terminal.
`curl   -d "client_id=leaderboard-ui" -d "client_secret=8090ed15-4cd1-483c-9fee-2a8b35941852"   -d "username=testemployee" -d "password=testemployee"   -d "grant_type=password"   "https://auth.knoldus.com/auth/realms/knoldus/protocol/openid-connect/token"`
2. You will recieve a response in json format, copy the following value `access_token`
3. When hitting endpoints using `Postman`
   1. Go to Authorization > Type: Bearer Token > Paste obtained token.
4. Now you endpoint calls are valid!


To hit any endpoint in knox, you will have to prefix endpoints with `v02` [At the time of writing this version of Readme].<br>
i.e `http://localhost:8000/v02/sessions?pageNumber=1&pageSize=10&filter=past` for example.

Here is an example of a response to `/sessions`

`{
"knolx": [
{
"id": "62bc6c453407e61970360b3a",
"presenterDetail": {
"fullName": "test admin",
"email": "testadmin@knoldus.com"
},
"coPresenterDetails": {
"fullName": "Someone",
"email": "Something"
},
"dateTime": 1656516219857,
"sessionDurationInMins": 45,
"topic": "Rust Fundamentals",
"category": "Rust",
"subCategory": "Scala Fundamentals",
"feedbackExpriationDate": 1656700199000,
"sessionType": "Knolx",
"sessionState": "PendingForAdmin",
"sessionDescription": "bdhfgakjfjfdajf",
"contentAvailable": false,
"content": {
"contentStatus": "NotAvailable"
}
}
],
"count": 1,
"pages": 1
}`
<br>

In this case we have a single previous knolx session under the email `testemployee@knoldus.com`
