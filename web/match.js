var match = {};
var USER_MATCHES = 40; // rate limit our matches
var DISTANCE_MATCH = 6371000; // EVVVERRRRYYYOOOONNEEEEEE    //16*1000.0; // 16km

var MATCHED = "matched";
var APPROVED = "approved";
var LIKED = "liked";
var DISLIKED = "disliked";
var REJECTED = "rejected";
var DECLINED = "declined";

var utils = require("cloud/utils");

/** Converts numeric degrees to radians */
if (typeof(Number.prototype.toRadians) === "undefined") {
  Number.prototype.toRadians = function() {
    return this * Math.PI / 180;
  }
}

function calculateAge(birthday)
{
  try {
    var month = birthday.split(' ')[0]
    var day = birthday.split(' ')[1].split(',')[0]
    var year = birthday.split(' ')[2]
    var today = new Date();
    var age = today.getFullYear() - year;
    if( today.getMonth() < month || ( today.getMonth()==month && today.getDate() < day ))
    {
      age--;
    }
    return age;
  } catch (err) {
    return 0; // error, we failed
  }  
}

function getPhoto(photo)
{
  var photourl = null;
  try {
    var photourl = photo.url();
  } catch ( e ) {}
  return photourl;
}

function displayUsers(users, res)
{
    // Take the first 40, get a name, age, and profile photo
    var displayList = [];
    for (i = 0; i < users.length; i++)
    {
      var user = {
        firstname: users[i]._serverData.firstname,
        age: calculateAge(users[i]._serverData.birthday),
        photo: getPhoto(users[i]._serverData.photo),
        updatedAt: users[i].updatedAt,
        id: users[i].id
      };
      displayList.push(user);
    }
    res.json({"success":true, users: displayList});
}

function getDistance(lat1, lon1, lat2, lon2)
{
  // check versus our own geolocation
  var R = 6371000; // metres
  var d1 = lat1.toRadians();
  var d2 = lat2.toRadians();
  var dlat = (lat2-lat1).toRadians();
  var dlon = (lon2-lon1).toRadians();
  var a = Math.sin(dlat/2) * Math.sin(dlat/2) +
          Math.cos(d1) * Math.cos(d2) *
          Math.sin(dlon/2) * Math.sin(dlon/2);
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

function findCloseUsers(user, users, res)
{
  // if ( !user._serverData.geolocation ) {
  //   res.json({"success":false, "message":"User has no geolocation, please enable GPS to continue."});
  //   return;
  // }

  // go through all users, find out if they are geolocated close to us
  // var closeUsers = [];
  // var lat1 = parseFloat(user._serverData.geolocation.split(',')[0]);
  // var lon1 = parseFloat(user._serverData.geolocation.split(', ')[1]);

  // for ( i = 0; i < users.length; i++ ) {
  //   var location = users[i]._serverData.geolocation;
  //   if ( location ){
  //     // 16km ~= 10 miles distance to a chinder match
  //     var lat2 = parseFloat(location.split(',')[0]);
  //     var lon2 = parseFloat(location.split(', ')[1]);
  //     var distance = getDistance(lat1, lon1, lat2, lon2);
  //     if ( distance < DISTANCE_MATCH )
  //     {
  //       closeUsers.push(users[i]);
  //     }
  //   }
  // }

  // FUCK IT WE'LL DO IT LIVE
  var closeUsers = users;

  // Eliminate everyone already in the swipe table  
  var swipeQuery = new Parse.Query(Parse.Object.extend("Swipes"));
  swipeQuery.equalTo("from", user.id);
  swipeQuery.limit(1000);
  var reformattedUsers = [];
  for(var i = 0; i < closeUsers.length; i++) {
     reformattedUsers.push(closeUsers[i].id);
  }

  // console.log("Looking for - " + reformattedUsers);
  swipeQuery.containedIn("to", reformattedUsers);
  swipeQuery.find({
    success: function(swipedUsers) {
      if ( swipedUsers.length > 0 )
      {
        // remove users we found that are in any state (already seen)
        swipedUsers.map(function(e){
          for ( var i = 0; i < closeUsers.length; i++)
          {
            if ( e._serverData.to == closeUsers[i].id )
            {
              closeUsers.splice(i,1);
              return;
            }
          }
        });
      }

      if ( closeUsers.length > USER_MATCHES )
      {
        closeUsers.splice(USER_MATCHES, (closeUsers.length-USER_MATCHES));
      }

      // now display remaining users
      if ( closeUsers.length > 0 ) {
       displayUsers(closeUsers, res);
      }
      else {
        res.json({"success":false, "message":"No chins found, chin up!"});
      }
    },
    failure: function(swipedUsers, error) {
      res.json({"success":false, "message":"Something went wrong"});
    }
  });  
}

match.latest = function(req, res)
{
  utils.authenticate(req, res, function(user){
    // Now lets find all users within our vicinity (how to do this??)
    var usersTable = new Parse.Query(Parse.Object.extend("Users"));
    usersTable.notEqualTo("objectId", user.id);
    usersTable.limit(1000);
    usersTable.find({
      success: function(users) {
        findCloseUsers(user, users, res);
      }
    });
  });
};

match.swipe = function(req, res)
{
  utils.authenticate(req, res, function(user){
    var action = req.param("action");
    var to = req.param("to");

    if ( (action !== "liked" && action !== "disliked") || to === undefined ){
      res.json({"success":false, "message":"invalid parameters for the swipe feature."});   
      return;
    }

    // check to see if this person has swiped on us already
    // Eliminate everyone already in the swipe table  
    var swipeQuery = new Parse.Query(Parse.Object.extend("Swipes"));
    swipeQuery.equalTo("from", to);
    swipeQuery.equalTo("to", user.id);
    swipeQuery.find({
      success: function(swipedUser) {
        if ( swipedUser.length > 0 )
        {
          var swipedAction = swipedUser[0]._serverData.action;
          // If this user has swiped on us, check the state
          if ( swipedAction == 'liked' )
          {
            // yeah buddy, we got a match
            swipedUser[0].set("action", APPROVED); // APPROVED
            swipedUser[0].save(null, {
              success: function( result ) {
                var SwipeObject = Parse.Object.extend("Swipes");
                var newSwipe = new SwipeObject();
                newSwipe.set("from", user.id);
                newSwipe.set("to", to);
                newSwipe.set("action", MATCHED); // MATCHED
                newSwipe.save(null, {
                  success: function (result) {
                    var toUser = new Parse.Query(Parse.Object.extend("Users"));
                    toUser.equalTo("objectId", to);
                    toUser.find({
                      success: function( foundUser ){
                        if ( foundUser.length == 1 )
                        {
                          // save our events and set up our DB
                          utils.saveEvent(swipedUser[0].id, {from: user.id, action:'match'});

                          res.json({"success":true,
                            "match":true,
                            "from": getPhoto(user._serverData.photo),
                            "to": getPhoto(foundUser[0]._serverData.photo),
                            "firstname": foundUser[0]._serverData.firstname
                          });
                        }
                        else
                        {
                          res.json({"success":false, "message": "Something bad happened..."});
                        }
                      },
                      failure: function( foundUser, error ) {
                        res.json({"success":false, "message": "Something bad happened..."});
                      }
                    });
                  },
                  failure: function( result ){
                    res.json({"success":false, "message": "Something bad happened..."});
                  }
                });
              },
              failure: function( result ){
                res.json({"success":false, "message": "Something bad happened..."});
              }
            });
          }
          else if ( swipedAction == 'disliked' )
          {
            swipedUser[0].set("action", DECLINED); // REJECTED
            swipedUser[0].save(null, {
              success: function( result ) {
                // yeah buddy, we got a match
                var SwipeObject = Parse.Object.extend("Swipes");
                var newSwipe = new SwipeObject();
                newSwipe.set("from", user.id);
                newSwipe.set("to", to);
                newSwipe.set("action", REJECTED);  // DECLINED
                newSwipe.save(null, {
                  success: function (result) {
                    res.json({"success":true, "match":false});
                  },
                  failure: function( result ){
                    res.json({"success":false, "message": "Something bad happened..."});
                  }
                });
              },
              failure: function( result ){
                res.json({"success":false, "message": "Something bad happened..."});
              }

            });
          }
          else
          {
            res.json({"success":false, "message": "Error, please try again later"});
          }
        }
        else {
          // user has not swiped on us yet, so we just saved this as liked or disliked
          var SwipeObject = Parse.Object.extend("Swipes");
          var newSwipe = new SwipeObject();
          newSwipe.set("from", user.id);
          newSwipe.set("to", to);
          newSwipe.set("action", action); // LIKED / DISLIKED
          newSwipe.save(null, {
            success: function( result ) {
              res.json({"success":true, "match":false});
            },
            failure: function( user, error ) {
              res.json({"success":false, "message": "Something bad happened..."});
            }
          });
        }
      },
      failure: function(swipedUser, error) {
        res.json({"success":false, "message": "Something bad happened..."});
      }
    }); 
  });
};

match.history = function(req,res)
{
  utils.authenticate(req,res, function(user) {
    // Now lets find all users within our vicinity (how to do this??)
    var usersTable = new Parse.Query(Parse.Object.extend("Users"));
    usersTable.notEqualTo("objectId", user.id);
    usersTable.limit(1000);
    usersTable.find({
      success: function(users) {

        // send the user every match they've had so far from our swiped table
        var swipeQuery = new Parse.Query(Parse.Object.extend("Swipes"));
        swipeQuery.equalTo("from", user.id);
        swipeQuery.containedIn("action", [MATCHED,APPROVED]);
        swipeQuery.descending("updatedAt");
        swipeQuery.limit(1000);
        swipeQuery.find({
          success: function(swipedUsers) {
            var finalUsers = [];
            swipedUsers.map(function(e){
              for ( var i = 0; i < users.length; i++)
              {
                if ( e._serverData.to === users[i].id )
                {
                  finalUsers.push(users[i]);
                  users.splice(i, 1);
                  break;
                }
              }
            });
            displayUsers(finalUsers, res);
          },
          failure: function(swipedUsers, error) {
            res.json({"success":false, "message": "Something bad happened..."});
          }
        });
      },
      failure: function(users, error) {
        res.json({"success":false, "message": "Something bad happened..."});
      }
    });
  });
};

module.exports = match;