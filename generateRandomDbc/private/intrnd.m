function [r] = intrnd(from, m, to)

%   intrnd() - Generate a random number in a given integer range. The shape of the
%                   distribution resembles the standard distribution.
%                     Caution: This is a simple, naive implementation with low performance.
%                   Don't use it to build long random vectors.
%                   
%   Input argument(s):
%       m           This is the result with the highest probability. The shape of the
%                   distribution is falling from here to either end of the range. m is in
%                   the range to..from, both including.
%       from, to    The integer range. The result r is in this range, both numbers
%                   including. from needs to be greater than to.
%
%   Return argument(s):
%       r           Integer intrnd number in the range from..to
%
%   Example(s):
%       clear p; for i=1:2000; p(i)=intrnd(1, 10, 100); end; hist(p,[1:100])
%       clear p; for i=1:2000; p(i)=intrnd(1, 50, 100); end; hist(p,[1:100])
%       clear p; for i=1:2000; p(i)=intrnd(1, 90, 100); end; hist(p,[1:100])
%       clear p; for i=1:2000; p(i)=intrnd(1, 100, 100); end; hist(p,[1:100])
%       assert(p >= 1  &&  p <= 100)
%       assert(intrnd(1, 4, 64) >= 1)
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

    
    assert(to >= from  &&  m >= from  && m <= to);
    
    w = max(m-from, to-m);
    
    do
        r = round(stdnormal_rnd(1,1) * (w/3) + m);
    until r >= from  &&  r <= to 
    
end % of function intrnd.


