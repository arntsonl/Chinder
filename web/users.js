var crypto = require('crypto');

var users = {};

var utils = require("cloud/utils");

function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

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

function checkFirstname(firstname) { 
  return /\b[A-Z]+\b/i.test(firstname);
}

function checkEmail(email) { 
  return /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}\b/i.test(email);
}

function checkPassword(password) {
  return (typeof(password) == 'string' && password.length >= 6);
}

function checkBirthday(birthday) {
  // Do a check for our birthday
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
    if ( age < 18 || age > 125 )
    {
      return false; // are we < 18 or > 125
    }
  } catch (err) {
    return false; // error, we failed
  }  

  return true;
}

function sendUserAttributes(result, res) {
  var photourl = null;
  try {
    var photourl = result._serverData.photo.url();
  } catch ( e ) {}
  // Execute any logic that should take place after the object is saved.
  res.json({'success':true,
    'firstname': result._serverData.firstname,
    'email': result._serverData.email,
    'birthday': result._serverData.birthday,
    'session': result._serverData.session,
    'geolocation': result._serverData.geolocation,
    'photo': photourl
  });
}

users.signup = function(req, res)
{
    var email = req.param("email");
    var firstname = req.param("firstname");
    var password = req.param("password");
    var birthday = req.param("birthday");
    var geolocation = req.param("geolocation");

    // Validate everything

    // Email - Regex
    // FirstName - no spaces
    // Password - at least 6?
    // birthday - Month Day, Year format must be consistant and valid

    if ( email == undefined ||
        firstname == undefined ||
        password == undefined ||
        birthday == undefined )
    {
        res.json({"success":false, message: "Invalid parameters for signup"});
        return;
    }
    else if ( checkEmail(email) == false )
    {
        res.json({"success":false, message: "Please use a valid email address"});
        return; 
    }
    else if ( checkFirstname(firstname) == false )
    {
        res.json({"success":false, message: "Please use a valid first name, no spaces or numbers"});
        return;  
    }
    else if ( checkPassword(password) == false )
    {
        res.json({"success":false, message: "Please use at least 6 characters for your password"});
        return;   
    }
    else if ( checkBirthday(birthday) == false )
    {
        res.json({"success":false, message: "You must be at least 18 to sign up to Chinder."});
        return;
    }

    var emailQuery = new Parse.Query(Parse.Object.extend("Users"));
    emailQuery.equalTo("email", email);
    emailQuery.find({
        success: function(results) {
            // send an error
            if ( results.length == 0 )
            {
              // Encrypt our password using AES256 and our secret password
              var encPassword = encrypt(password);

              // Write our new user
              var UserObject = Parse.Object.extend("Users");
              var user = new UserObject();
               
              var session = guid();

              user.set("email", email);
              user.set("firstname", firstname);
              user.set("password", encPassword);
              user.set("birthday", birthday);
              user.set("geolocation", geolocation);
              user.set("session", session);
               
              user.save(null, {
                success: function(newUser) {
                  sendUserAttributes(newUser, res);
                },
                error: function(result, error) {
                  // Execute any logic that should take place if the save fails.
                  // error is a Parse.Error with an error code and message.
                  res.json({'success':false, 'message': 'could not create user because ' + error.message});
                }
              });
            }
            else
            {
              res.json({'success':false, 'message': 'Email already in use, please choose another'});
            }
        }
    });
};

users.login = function(req, res)
{
    var email = req.param("email");
    var password = req.param("password");

    if ( email == undefined ||
        password == undefined )
    {
        res.json({'success':false, 'message': 'invalid parameters for login'});
        return;
    }

    // Encrypt our password using AES256 and our secret password
    var encPassword = encrypt(password);
    var loginQuery = new Parse.Query(Parse.Object.extend("Users"));
    loginQuery.equalTo("email", email);
    loginQuery.equalTo("password", encPassword);
    loginQuery.find({
      success: function(user) {
        if ( user.length == 1 )
        {
          var session = guid();
          user[0].set("session", session);
          user[0].save(null, {
            success: function(savedUser) {
              sendUserAttributes(savedUser, res);
            },
            error: function(result, error) {
              // Execute any logic that should take place if the save fails.
              // error is a Parse.Error with an error code and message.
              res.json({'success':false, 'message': 'could not update session because ' + error.message});
            }
          });
        }
        else
        {
          res.json({"success":false, "message":"Missing email or incorrect password"});
        }
      }
    });
};

users.authenticate = function(req, res)
{
  utils.authenticate(req, res, function(user){
    // Now lets find all users within our vicinity (how to do this??)
    sendUserAttributes(user, res);
  });
};

users.update = function(req, res)
{
    utils.authenticate(req, res, function(user) {
      var firstname = req.param("firstname");
      var birthday = req.param("birthday");
      var password = req.param("password");
      if ( firstname == null && birthday == null && password == null)
      {
          res.json({"success":false, message: "Missing param for update"});
          return;
      }
      else if ( firstname && checkFirstname(firstname) == false )
      {
          res.json({"success":false, message: "Please use a valid first name, letters only please"});
          return;  
      }
      else if ( password && checkPassword(password) == false )
      {
          res.json({"success":false, message: "Please use at least 6 characters for your password"});
          return;   
      }
      else if ( birthday && checkBirthday(birthday) == false )
      {
          res.json({"success":false, message: "You must be at least 18 to sign up to Chinder."});
          return;
      }


      // The file has been saved to Parse.
      if ( firstname )
      {
        user.set("firstname", firstname); 
      }
      if ( birthday ) 
      {
        user.set("birthday", birthday);
      }
      if ( password ) 
      {
        var encPassword = encrypt(password);
        user.set("password", encPassword);
      }

      user.save(null, {
        success: function(result) {
          sendUserAttributes(result, res);
        },
        error: function(result, error) {
          // Execute any logic that should take place if the save fails.
          // error is a Parse.Error with an error code and message.
          res.json({'success':false, 'message': 'could not update user because ' + error.message});
        }
      });
    });
};

users.uploadphoto = function(req, res)
{
  utils.authenticate(req, res, function(user) {
    var photo = req.param("photo");
    if ( photo == undefined)
    {
        res.json({'success':false, 'message': 'invalid parameters for authenticate'});
        return;
    }

    var fileName = guid() + ".png";
    var photoFile = new Parse.File(fileName, { base64: photo });
    photoFile.save().then(function() {

      // The file has been saved to Parse.
      user.set("photo", photoFile); 
      user.save(null, {
        success: function(result) {
          // Execute any logic that should take place after the object is saved.
          sendUserAttributes(result, res);
        },
        error: function(result, error) {
          // Execute any logic that should take place if the save fails.
          // error is a Parse.Error with an error code and message.
          res.json({'success':false, 'message': 'could not create user because ' + error.message});
        }
      });
    }, function(error) {
      // The file either could not be read, or could not be saved to Parse.
      res.json({"success":false, "message":"Could not upload image, try again later."});
    });
  });
};

users.updatelocation = function(req, res) {
  utils.authenticate(req, res, function(user) {
    var geolocation = req.param("geolocation");
    if ( geolocation == undefined)
    {
        res.json({'success':false, 'message': 'invalid parameters for update location'});
        return;
    }

    // The file has been saved to Parse.
    user.set("geolocation", geolocation); 
    user.save(null, {
      success: function(result) {
        // Execute any logic that should take place after the object is saved.
        sendUserAttributes(result, res);
      },
      error: function(result, error) {
        // Execute any logic that should take place if the save fails.
        // error is a Parse.Error with an error code and message.
        res.json({'success':false, 'message': 'could not set geolocation because ' + error.message});
      }
    });

  });
};

module.exports = users;