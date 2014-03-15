#!/usr/bin/ruby

# Script to call Java code to get latest version of data from Overpass API and load onto Google Site.
require 'optparse'
require 'net/smtp'

username = ARGV[0]
password = ARGV[1]
filename = ARGV[2]

log = `java -cp ./dist/UpdateJsonOnSite.jar:./dependencies/* coinTraveler.UpdateJsonOnSite #{username} #{password} #{filename}`
result = $?.exitstatus

output = ""
log.each_line{|line| output = line + "<br>" + output }
log = output

# If didn't fetch new file from API then just quit without email.
if result == 100
    exit
end

# Create email.
message = <<MESSAGE_END
From: Coin Traveler <CoinTraveler>
To: <#{username}@gmail.com>
MIME-Version: 1.0
Content-type: text/html
Subject: [COIN] Update Completed - #{Time.now().strftime("%m-%d-%Y")}

<br>
Coin Traveler node update ran at #{Time.now().strftime("%m-%d-%Y %H:%M")}.
<br>
<p>
Log messages:
<blockquote>
<font size="1" face="courier">
#{log}
</font>
</blockquote>
</p>
MESSAGE_END

# Send email.
smtp = Net::SMTP.new 'smtp.gmail.com', 587
smtp.enable_starttls
smtp.start("YourDomain", "#{username}@gmail.com", password, :login) do
        smtp.send_message(message, "cointraveler", "#{username}@gmail.com" )
end
