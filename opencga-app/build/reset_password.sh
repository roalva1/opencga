#!/bin/bash

message="Hello, \\nYou can now login using this new password:\\n\\n$2\\n\\n\\nPlease change it when you first login\\n\\nBest regards,\\n\\nSystems Genomics Laboratory\\n"
echo -e $message | mail -s "Your password has been reset" $1

