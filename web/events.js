var events = {};

var utils = require("cloud/utils");

events.poll = function(req, res)
{
  utils.authenticate(req, res, function(user){
    console.log("User is requesting an event poll - " + user.id);

    // check our event queue, if we found anything, return either messages or matches
    var eventQuery = new Parse.Query(Parse.Object.extend("EventQueue"));
    eventQuery.equalTo("to", user.id);
    eventQuery.limit(1000);
    eventQuery.find({
      success: function(events) {
        console.log("Found events - " + events);
        var eventData = [];
        for( var i = 0; i < events.length; i++ )
        {
          eventData.push(events[i]._serverData.data);
          console.log("Removing events")
          events[i].destroy();
        }
        console.log(eventData); // debug 
        res.json({"success":true, "events": eventData});
        // set all of our acquired events viewed to true
      },
      failure: function(error) {
        res.json({"success":false, "message": "Cannot poll events at this time"});
      }
    });
  });
};

module.exports = events;