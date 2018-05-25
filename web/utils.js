var utils = {};

utils.authenticate = function(req, res, callback)
{
  var session = req.param("session");

  if ( session == undefined )
  {
      res.json({'success':false, 'message': 'invalid parameters for authenticate'});
      return;
  }

  // Encrypt our password using AES256 and our secret password
  var sessionQuery = new Parse.Query(Parse.Object.extend("Users"));
  sessionQuery.equalTo("session", session);
  sessionQuery.find({
    success: function(newUser) {
      if ( newUser.length == 1 )
      {
        callback(newUser[0]);
      }
      else
      {
        res.json({"success":false, "message":"Could not authenticate, please login again."});
      }
    }
  });
}

utils.saveEvent = function(userId, data)
{
  var EventObject = Parse.Object.extend("EventQueue");
  var newEvent = new EventObject();
  newEvent.set("to", userId);
  newEvent.set("data", data);
  newEvent.save(null, {
    success: function(newUser) {
      // Tell the user we sent a message
    },
    error: function(result, error) {
      console.log("Could not save event!");
    }
  });
}

module.exports = utils;