# Postman Testing

## Setup
Ensure that your psql db is set up correctly as described in the README in the root directory.

Add a user (either by manual insertion or sending a POST request to sign up a user) with the email of "admin@email.com" and password "secret".

Then run the collection by importing it with the Postman desktop or web app, or run it on the command line with `newman` which can be installed with `npm install -g newman`.