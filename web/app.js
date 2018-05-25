
// These two lines are required to initialize Express in Cloud Code.
var express = require('express');
var app = express();

var users = require('cloud/users');
var match = require('cloud/match');
var messages = require('cloud/messages');
var events = require('cloud/events');

// experimental
var experimentalUsers = require('cloud/experimental/users');
var experimentalMatch = require('cloud/experimental/match');
var experimentalMessages = require('cloud/experimental/messages');
var experimentalEvents = require('cloud/experimental/events');

// Global app configuration section
app.set('views', 'cloud/views');  // Specify the folder to find templates
app.set('view engine', 'ejs');    // Set the template engine
app.use(express.bodyParser());    // Middleware for reading request body

// This is an example of hooking up a request handler with a specific request
// path and HTTP verb using the Express routing API.

app.post('/api/users/signup', function(req,res) {
  users.signup(req, res);
});

app.post('/api/users/login', function(req,res) {
  users.login(req, res);
});

app.post('/api/users/reset', function(req, res) {
  users.resetUser(req, res);
});

app.post('/api/users/authenticate', function(req, res) {
  users.authenticate(req, res);
});

app.post('/api/users/update', function(req, res) {
  users.update(req, res);
});

app.post('/api/users/uploadphoto', function(req, res) {
  users.uploadphoto(req, res);
});

app.post('/api/users/location', function(req, res) {
  users.updatelocation(req, res);
});

app.post('/api/match/latest', function(req, res) {
  match.latest(req, res);
});

app.post('/api/match/swipe', function(req, res) {
  match.swipe(req, res);
});

app.post('/api/match/history', function(req, res) {
  match.history(req, res);
});

app.post('/api/messages/list', function(req, res) {
  messages.list(req, res);
});

app.post('/api/messages/history', function(req, res) {
  messages.history(req, res);
});

app.post('/api/messages/send', function(req, res) {
  messages.send(req, res);
});

app.post('/api/events/poll', function(req, res) {
  events.poll(req, res);
});

/*  EXPERIMENTAL ENDPOINTS */
app.post('/experimental/users/signup', function(req,res) {
  experimentalUsers.signup(req, res);
});

app.post('/experimental/users/login', function(req,res) {
  experimentalUsers.login(req, res);
});

app.post('/experimental/users/reset', function(req, res) {
  experimentalUsers.resetUser(req, res);
});

app.post('/experimental/users/authenticate', function(req, res) {
  experimentalUsers.authenticate(req, res);
});

app.post('/experimental/users/update', function(req, res) {
  experimentalUsers.update(req, res);
});

app.post('/experimental/users/uploadphoto', function(req, res) {
  experimentalUsers.uploadphoto(req, res);
});

app.post('/experimental/users/location', function(req, res) {
  experimentalUsers.updatelocation(req, res);
});

app.post('/experimental/match/latest', function(req, res) {
  experimentalMatch.latest(req, res);
});

app.post('/experimental/match/swipe', function(req, res) {
  experimentalMatch.swipe(req, res);
});

app.post('/experimental/match/history', function(req, res) {
  experimentalMatch.history(req, res);
});

app.post('/experimental/messages/history', function(req, res) {
  experimentalMessages.history(req, res);
});

app.post('/experimental/messages/send', function(req, res) {
  experimentalMessages.send(req, res);
});

app.post('/experimental/events/poll', function(req, res) {
  experimentalEvents.poll(req, res);
});

// Attach the Express app to Cloud Code.
app.listen();
