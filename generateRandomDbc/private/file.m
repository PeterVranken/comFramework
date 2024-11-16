function [fileContents] = file(fileName, textToWrite)

%   file() - Simple File I/O
%                   
%   Input argument(s):
%       fileName    The name of the file to read or write
%       textToWrite The contents of a written file.
%                     Note, the file is opened in binary mode, which means that the EOL
%                   character conversion is not applied. The text contents already need to
%                   use the EOL character sequence, which is suitable for the underlaying
%                   system.
%                     This argument is optional. Not using it
%                   means to use the function in read direction - a text file is read and
%                   the contents are returned
%
%   Throws:
%                   An exception is thrown in case of any file I/O error. The affected
%                   files/file contents are not usable in this case
%
%   Return argument(s):
%       fileContents  The text contents read from file if second argument is not given
%
%   Example(s):
%       myInputText = file('/tmp/mySourceFile.fcl');
%       file('/tmp/myCompiledProgram.c', textToWrite);
%
%   Copyright (C) 2016-2017 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
    noPar = 1;

    % Number of optional parameters.
    noOptPar = 1;

    error(nargchk(noPar, noPar+noOptPar, nargin));

    if nargin == 1
        % Read text file
        [fid msg] = fopen(fileName, 'rt');
        if fid == -1
            error(['Can''t open input file ' fileName '. ' msg]);
        end
        [fileContents count] = fread(fid);
        if count <= 0
            warning(['No character has been read from input file ' fileName '. File empty?']);
        else
            % Shape a row column of characters, a normal string.
            fileContents = char(fileContents.');
        end
        if fclose(fid) == -1
            warning(['Can''t close input file ' fileName]);
        end
    else
        assert(nargout == 0, 'No output is produced in case of writing a file')
        
        % Write file. Although we have a text file we need to open the file binary: The
        % StringTemplate V4 engine already does do the EOL conversion.
        [fid msg] = fopen(fileName, 'wb');
        if fid == -1
            error(['Can''t open file ' fileName ' for writing. ' msg]);
        end
        [count] = fwrite(fid, textToWrite);
        if count ~= length(textToWrite)
            error(['Error writing file ' fileName '. File will be unusable']);
        end
        if fclose(fid) == -1
            error(['Can''t close output file ' fileName '. File might be unusable']);
        end
    end
end % of function file.


