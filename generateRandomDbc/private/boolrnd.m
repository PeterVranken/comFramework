function [b] = boolrnd(p, r, c)

%   boolrnd() - Generate a Boolean random matrix.
%                   
%   Input argument(s):
%       p           The likelihood of a true in the range [0..1].
%                     Please note, the documentation of the underlaying function unifrnd
%                   says that the upper boundary of the range of the generated random
%                   numbers may is including. We can't therefore guarantee that b==true is
%                   always true for p==1.
%       r,c         The size of the generated matrix. r and c are optional. The default for
%                   each is one.
%
%   Return argument(s):
%       b           The vector with randomly chosen Boolean values.
%
%   Example(s):
%       p=boolrnd(0.2,1000,1); hist(p, [0 1]);
%       p=boolrnd(0,1000,1); assert(p == false);
%       p=boolrnd(1,1000,1); find(p~=true) % Note, assert(p == true) is not granted
%
%   Copyright (C) 2016 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
    noOptPar = 2;

    error(nargchk(noPar, noPar+noOptPar, nargin));

    % Set the optional parameter values.
    noPar = noPar + 1;
    if nargin < noPar
        r = 1;
    end
    noPar = noPar + 1;
    if nargin < noPar
        c = 1;
    end
    b = unifrnd(0, 1, r, c);
    b = b < p;
    
end % of function boolrnd.


