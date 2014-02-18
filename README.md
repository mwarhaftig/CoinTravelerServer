Java script to download JSON from Overpass API file and upload to Google Site.  Wrapped in  a Ruby script which handles calling the Java code and email out the results.  All notes assume command being run from CoinTravelerServer repo's base directory and Linux based OS.

Requires Java 1.7 & Ruby 2.X.


To run update via Ruby wrapper and include email notification:
./dist/UpdateJsonOnSiteAndEmail.rb GoogleUsername GooglePassword ./BitcoinMapNodes.json

Parameters:
1. Google account username (and Google Site domain name).
2. Google account password.
3. Local path for file.  The file's name will be what you want to update on Google Site.    


To run up the Java updater without email notification:
java -cp ./dist/UpdateJsonOnSite.jar:./dependencies/ coinTraveler.UpdateJsonOnSite GoogleUsername GooglePassword ./BitcoinMapNodes.json

Parameters:
1. Google account username (and Google Site domain name).
2. Google account password.
3. Local path for file.  The file's name will be what you want to update on Google Site.    


To create a new jar after updating Java code:
jar cfm ./dist/UpdateJsonOnSite.jar Manifest.txt coinTraveler/UpdateJsonOnSite.class