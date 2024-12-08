#!/usr/bin/env PowerShell
# Open a PowerShell window in interactive mode and with the environment properly prepared for
# executing the build commands for makingthe tools, running the samples and creating a
# release archive.
#
# Please note: On Windows machines, the use of PowerShell is initially hindered by two
# stupid settings, but normal user rights permit to fully enable its use. 
# 
# First, the use of PowerShell is by default restricted to interactive use but script
# execution is forbidden. To overcome this, open a PowerShell window interactively and
# type:
#
#   Set-ExecutionPolicy unrestricted -Scope CurrentUser
# 
# The effect of the command is permanent. You can close your PowerShell window and start
# using our PowerShell scripts.
#
# Second, the default operation on mouse-click is opening the script in a text editor
# instead of running the script in PowerShell. You should change the file name assciation,
# e.g., by typing "Default apps" in the start menu and looking for the settings of file
# type ".ps1". Associate either pwsh.exe or PowerShell.exe with this file type.
#
# Copyright (c) 2024, Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
#
# This program is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the
# Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with this program. If not, see <http://www.gnu.org/licenses/>.

# Which executable is currently executing the launch script?
$sh = (Get-Process -Id $PID).Path

# Start same executable with appropriate session configuration.
.$sh -WindowStyle Normal -Interactive -NoExit -Command {
    setEnv.ps1
    $env:PATH = "$env:JAVA_HOME\bin;$env:ANT_HOME\bin;$env:GITWCREV_HOME;$PSScriptRoot;.;$env:PATH"
    #write-host "PATH: $env:PATH"
    pushd $PSScriptRoot
    dir *.ps1
    write-host ("To get first help, type:`n" `
                + "  buildCodeGenerator -p`n" `
                + "  buildExcelExporter -p`n" `
                + "  canInterface -p`n" `
                + "  zipComFramework -p")
    write-host ("To get more help, type:`n" `
                + "  start https://github.com/PeterVranken/comFramework/wiki")
}