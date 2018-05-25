var messages = {};

var utils = require("cloud/utils");

var MATCHED = "matched";
var APPROVED = "approved";

// Nodejs encryption with CTR
var crypto = require('crypto'),
    algorithm = 'aes-256-ctr',
    password = 'NOTTHEREALPASSWORD';
 
function encrypt(text){
  var cipher = crypto.createCipher(algorithm,password)
  var crypted = cipher.update(text,'utf8','hex')
  crypted += cipher.final('hex');
  return crypted;
}

function decrypt(text){
  var decipher = crypto.createDecipher(algorithm,password)
  var dec = decipher.update(text,'hex','utf8')
  dec += decipher.final('utf8');
  return dec;
}

function getPhoto(photo)
{
  var photourl = null;
  try {
    var photourl = photo.url();
  } catch ( e ) {}
  return photourl;
}

messages.history = function(req, res)
{
  utils.authenticate(req,res, function(user) {
    var toId = req.param("to");
    if ( toId == undefined ) {
        res.json({"success":false, message: "Invalid parameters for message history"});
        return;
    }

    // Get our to user
    var toUserQuery = new Parse.Query(Parse.Object.extend("Users"));
    toUserQuery.equalTo("objectId", toId);
    toUserQuery.limit(1);
    toUserQuery.find({
      success: function(foundUser) {

        if ( foundUser.length === 1 ) {
          // Get a list of the messages
          var messageQuery = new Parse.Query(Parse.Object.extend("Messages"));
          messageQuery.containedIn("from", [user.id, toId]);
          messageQuery.containedIn("to", [user.id, toId]);
          messageQuery.ascending("createdAt"); // order by date
          messageQuery.limit(1000);

          messageQuery.find({
            success: function(allMessages) {
              var messageList = [];
              allMessages.map(function(message) {
                messageList.push({
                  text: decrypt(message._serverData.message),
                  sent: message.createdAt,
                  origin: message._serverData.from
                });
              });
              res.json({"success":true, "photo": getPhoto(foundUser[0]._serverData.photo), "messages":messageList});
            },
            failure: function(fromMessages, error) {
              res.json({"success":false, "message":"Cannot retrieve messages at this time"});
            }
          });
        }
        else {
          res.json({"success":false, "message":"Could not find the user"});  
        }
      },
      failure: function(foundUser, error) {
        res.json({"success":false, "message":"Cannot retrieve messages at this time"});
      }
    });
  });
};

messages.send = function(req, res)
{
  utils.authenticate(req,res, function(user) {
    // Send a message, get any pending messages back in the request
    var toId = req.param("to");
    var message = req.param("message");
    if ( toId == undefined || message == undefined ) {
      res.json({"success":false, message: "Invalid parameters for message send"});
      return;
    }

    // No unicode please
    var messageFormatted = message.replace(/[\uE000-\uF8FF]/g, '');
    console.log("Message Formatted - " + messageFormatted);
    var messageEncrypted = encrypt(messageFormatted);
    console.log("Message Encrypted - " + messageEncrypted);

    // Get a list of the messages
    var messageToQuery = new Parse.Query(Parse.Object.extend("Swipes"));
    messageToQuery.equalTo("from", user.id);
    messageToQuery.equalTo("to", toId);
    messageToQuery.containedIn("action", [APPROVED, MATCHED]);
    messageToQuery.limit(1000);
    messageToQuery.find({
      success: function(matched) {
        console.log("Found the following users matched - " + matched.length);
        if ( matched.length == 1 ) {
          // Check and make sure this user has actually matched with the user in question
          var MessageObject = Parse.Object.extend("Messages");
          var message = new MessageObject();
          message.set("from", user.id);
          message.set("to", toId);
          message.set("message", encrypt(messageFormatted));
          message.save(null, {
            success: function(newUser) {
              // Tell the user we sent a message
              console.log("Saving message to user - " + toId);
              utils.saveEvent(toId, {from: user.id, action:'message'});

              res.json({'success':true});
            },
            error: function(result, error) {
              // Execute any logic that should take place if the save fails.
              // error is a Parse.Error with an error code and message.
              res.json({'success':false, 'message': 'Please try again later'});
            }
          });
        }
        else
        {
          res.json({"success":false, "message":"You are not currently matched with this user"});
        }
      },
      failure: function(matched, error) {
        res.json({"success":false, "message":"Cannot send message at this time"});
      }
    });
  });
};

module.exports = messages;