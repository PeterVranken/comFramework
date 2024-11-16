function dbc=generateRandomDbc(fileName, ecuName, noFrames)

%   generateRandomDbc() - Generate a random CAN network database file, which may be useful
%                   for testing purpose. 
%
%   Input argument(s):
%       fileName    The name of the generated network database (*.dbc) file
%       ecuName     The name of the ECU, which the database is generated for
%       noFrames    The number of frames in the database. Optional, default is 100
%
%   Throws:
%                   All problems, which inhibit the generation of the file are reported by
%                   error
%
%   Return argument(s):
%       dbc         The internal representation of the generated DBC file is returned as
%                   nested data structure
%
%   Example(s):
%       generateRandomDbc('myTestCanDatabase.dbc', 'myEcu');
%
%   Copyright (C) 2017 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
%
%   This program is free software: you can redistribute it and/or modify it
%   under the terms of the GNU General Public License as published by the
%   Free Software Foundation, either version 3 of the License, or (at your
%   option) any later version.
%
%   This program is distributed in the hope that it will be useful, but
%   WITHOUT ANY WARRANTY; without even the implied warranty of
%   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
%   General Public License for more details.
%
%   You should have received a copy of the GNU General Public License along
%   with this program. If not, see <http://www.gnu.org/licenses/>.

    % Number of mandatory parameters.
    noPar = 2;

    % Number of optional parameters.
    noOptPar = 1;

    error(nargchk(noPar, noPar+noOptPar, nargin));

    % Set the optional parameter values.
    noPar = noPar + 1;
    if nargin < noPar
        noFrames = 100;
    end
    
    % Clear persistent data of sub-function. The 11 Bit CAN ID space is reopened.
    clear randomFrame

    dbc = struct( 'name', fileName ...
                , 'date', date ...
                , 'ecu', ecuName ...
                );
    dbc.frameAry = {};
    for i=1:noFrames
        dbc.frameAry{end+1} = randomFrame(true);
    end
    fileContents = st4Render('dbc.stg', 'dbcFile', 'dbc', dbc);
    file(fileName, fileContents);

end % of function generateRandomDbc.


