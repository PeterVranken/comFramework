function [frame] = randomFrame(checksumRequired)

% Randomly create the data structure that describes a possible CAN frame.
%   Throws:
% All kind of errors, which inhibit the creation of a frame object are reported by error.
%   Return value frame:
% The data structure with all information about the frame.
%   Parameter checksumRequired:
% If true then all frames will contain a special signal, suitable to hold a checksum. The
% signal has 8 Bit length and is placed on a byte boundary. The name begins with
% "checksum".

% Copyright (C) 2017 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
%
% This program is free software: you can redistribute it and/or modify it
% under the terms of the GNU General Public License as published by the
% Free Software Foundation, either version 3 of the License, or (at your
% option) any later version.
%
% This program is distributed in the hope that it will be useful, but
% WITHOUT ANY WARRANTY; without even the implied warranty of
% MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
% General Public License for more details.
%
% You should have received a copy of the GNU General Public License along
% with this program. If not, see <http://www.gnu.org/licenses/>.

    % Define the sets of frame and signal property values. Actual values are chosen
    % randomly from these sets.
    setOfSendTypes    = {'regular' 'mixed' 'event'};
    setOfSendTimes    = [5 10 20 25 50 100 500 1000 10000]; % ms
    setOfMinDistances = [50 100 500 1000]; % ms
    setOfUnits        = {'A' 'V' 'km/h' 'W' 'kWh' 's' 'm/s^2'};
    
    % Choose CAN ID.
    persistent canIdAry
    if isempty(canIdAry)
        canIdAry = 0:2047;
    end
    idxCanId = unidrnd(length(canIdAry));
    canId = canIdAry(idxCanId);
    canIdAry = [canIdAry(1:idxCanId-1) canIdAry(idxCanId+1:end)];
    if isempty(canIdAry)
        % This decision is not fully correct; the last available ID is unusable.
        error(['Can''t generate a random frame. Index range of 11 Bit CAN IDs is exhausted']);
    end

    % Select length of frame.
    dlc = intrnd(0, 8, 8);

    sendType = setOfSendTypes{intrnd(1, 1, length(setOfSendTypes))};
    switch sendType
    case 'regular'
        sendTime = setOfSendTimes(unidrnd(length(setOfSendTimes)));
        minDistance = [];
    case 'mixed'
        minDistance = setOfMinDistances(unidrnd(length(setOfMinDistances)));
        do
            % +-4: Don't use fast regular times for event triggered frames.
            sendTime = setOfSendTimes(unidrnd(length(setOfSendTimes)-4)+4);
        until sendTime >= minDistance
    case 'event'
        sendTime = [];
        minDistance = setOfMinDistances(unidrnd(length(setOfMinDistances)));
    otherwise assert(false)
    end
    
    frame = struct( 'name', ['frame_' num2str(canId)] ...
                  , 'comment', 'Randomly defined CAN frame' ...
                  , 'id', uint32(canId) ...
                  , 'size', uint16(dlc) ...
                  , 'isReceived', boolrnd(0.67) ...
                  , 'sendType', sendType ...
                  , 'sendTime', uint32(sendTime) ...
                  , 'minDistance', uint32(minDistance) ...
                  , 'crcStartValue', uint8(unidrnd(255)) ...
                  );
    frame.signalAry = {};
    
    % Compose the set of signals for a non empty frame.
    lenOfWord = 8;
    if dlc > 0
        % Endianess: Either chosen for whole frame or per signal.
        haveFrameEndianess = boolrnd(0.25);
        isFrameMotorola = boolrnd(0.5);

        if checksumRequired
            % If a signal has the meaning of a checksum then some restrictions apply, which
            % disable full random generation. Generate a checksum signal separately: 8 Bit
            % on a byte boundary to enable a simple implementation of the common CRC8
            % protection.
            idxChkSumByte = unidrnd(dlc);
            start = 8*(idxChkSumByte-1)+1;
            
            % The DBC start bit is zero based and depends on the endianess.
            if haveFrameEndianess
                isMotorola = isFrameMotorola;
            else
                isMotorola = boolrnd(0.5);
            end
            startBit = start-1;
            if isMotorola
                startBit = transformGrid(startBit);
            end
            % Add the signal to the frame.
            signal = struct( 'name', ['checksum_' num2str(start) '_' num2str(8)] ...
                           , 'comment' ...
                             , '8 Bit checksum for protection of frame contents' ...
                           , 'length', uint16(8) ...
                           , 'isSigned', false ...
                           , 'factor', 1 ...
                           , 'offset', 0 ...
                           , 'unit', '' ...
                           , 'min', 0 ...
                           , 'max', 255 ...
                           , 'initialValue', 0 ...
                           , 'startBit', uint16(startBit) ...
                           , 'isMotorola', isMotorola ...
                           );
            frame.signalAry{end+1} = signal;
            
            % Specifiy the remaining bit set.
            setOfFreeBits = [0:(idxChkSumByte-1)*lenOfWord-1 ...
                             idxChkSumByte*lenOfWord:dlc*lenOfWord-1 ...
                            ];
        else
            % Initialize bit area as a single chunk.
            setOfFreeBits = (1:dlc*lenOfWord)-1;
        end        
        
        % Find the sequence of consequtive free bits, which start with the first free bit
        % in setOfFreeBits.
        function s = getFreeSequence(isMotorola)

            % Iterate the bit index in the implementation order, always starting with a
            % possible signal start bit.
            %   Get the zero based bit index or -1 if the iteration reached to end of all
            % bits.
            %   isByteOrderMotorola is optional. Passing it means to start a new iteration.
            % The function returns []. Otherwise get the next index from the running
            % iteration.
            function idxBit = iterateBits(isByteOrderMotorola)
                persistent isMotorola nextIdx
                if nargin >= 1
                    % Start a new iteration
                    isMotorola = isByteOrderMotorola;
                    if isMotorola
                        nextIdx = lenOfWord-1;
                    else
                        nextIdx = 0;
                    end
                    idxBit = [];
                else
                    % Iteration is running.
                    if nextIdx < dlc*lenOfWord;
                        % Return next index and compute successor.
                        idxBit = nextIdx;
                        if isMotorola
                            if mod(nextIdx, lenOfWord) == 0
                                nextIdx = nextIdx + 2*lenOfWord-1;
                            else
                                nextIdx = nextIdx - 1;
                            end
                        else
                            nextIdx = nextIdx + 1;
                        end
                    else
                        % Iteration has terminated.
                        idxBit = -1;
                    end
                end
            end % iterateBits

            % Start iteration along all bits in the frame.
            iterateBits(isMotorola);

            % Look for the first free bit.
            do
                idxBit = iterateBits;
            until idxBit < 0  || ismember(idxBit, setOfFreeBits)

            % Collect all sub-sequent free bits in s.
            s = [];
            while ismember(idxBit, setOfFreeBits)
                s(end+1) = idxBit;
                idxBit = iterateBits;
            end
        end % getFreeSequence

        % Iterate until all remaining free bits are filled with signals.
        while ~isempty(setOfFreeBits)

            % Randomly chose endianess unless the frame has one and the same for all signals.
            if haveFrameEndianess
                isMotorola = isFrameMotorola;
            else
                isMotorola = boolrnd(0.5);
            end

            % TODO It could be an idea to get the sequences for both endianesses and decide
            % for the endianess with the larger sequence length (instead of random). This
            % would lead to longer signals but it could mean to significantly reduce the
            % likelihood of having mixed orders inside one frame. Try.
            sequenceOfFreeBits = getFreeSequence(isMotorola);
            lenArea = length(sequenceOfFreeBits);
            assert(lenArea > 0);
            
            % Select the length of a signal in the given area.
            targetLength = min(13, lenArea);
            lenSig = intrnd(1, targetLength, lenArea);

            % Select the position of the signal in the area. start relates to the element
            % in sequenceOfFreeBits.
            if lenArea > lenSig
                start = unidrnd(lenArea-lenSig+1);
            else
                start = 1;
            end
            
            % The zero based start bit of the chosen signal.
            startBit = sequenceOfFreeBits(start);

            % Update the set of free bits.
            setOfFreeBits = setdiff(setOfFreeBits, sequenceOfFreeBits(start:start+lenSig-1));

            % Leave a little chance to not use the selected area. Many CAN frames have some
            % space left. The probability of leaving blank should drop with the length of
            % the area.
            if lenSig >= 8
                pBlank = 0.03;
            else
                pBlank = (8-lenSig)*0.03;
            end
            if boolrnd(pBlank)
                continue
            end

            % Signal properties: Scaling. Integer signals are always assumed for very short
            % and not too long signals. Otherwise it's a random decision.
            if lenSig >= 4  && (lenSig > 20  || boolrnd(0.67))
                % Scaled signal. Choose scaling such that a reasonable not too large or too
                % little range appears. The decisions are made roughly, signedness is e.g.
                % not considered although it has a significant impact on the range.
                targetMagnitude = unidrnd(11) - 6;
                factor = 10^round(targetMagnitude - lenSig/log2(10));
                offset = 10^(targetMagnitude-0.5) * (unidrnd(3)-2);
                unit = setOfUnits{unidrnd(length(setOfUnits))};
            else
                % Integer quantity with identy scaling
                factor = 1;
                offset = 0;
                unit = '';
            end
            
            % Signal properties: Signedness and range
            isSigned = lenSig >= 2  && boolrnd(0.5);
            if isSigned
                sigMin = - 2^(lenSig-1);
                sigMax = 2^(lenSig-1) - 1;
            else
                sigMin = 0;
                sigMax = 2^lenSig - 1;
            end
            if lenSig >= 3
                % Consider special coded values like "invalid". These are usually placed at
                % the positive end of the range.
                sigMax = sigMax -1;
            end
            initialValue = sigMin + unidrnd(sigMax-sigMin+1)-1;
            sigMin = factor*sigMin + offset;
            sigMax = factor*sigMax + offset;
            initialValue = factor*initialValue + offset;
            
            % Add the signal to the result.
            signal = struct( 'name', ['sig_' num2str(startBit) '_' num2str(lenSig)] ...
                           , 'comment', [] ...
                           , 'length', uint16(lenSig) ...
                           , 'isSigned', isSigned ...
                           , 'factor', factor ...
                           , 'offset', offset ...
                           , 'unit', unit ...
                           , 'min', sigMin ...
                           , 'max', sigMax ...
                           , 'initialValue', initialValue ...
                           , 'startBit', uint16(startBit) ...
                           , 'isMotorola', isMotorola ...
                           );
            frame.signalAry{end+1} = signal;
            
        end % while Still remaining free bits left
        
    end % if frame is not empty    
    
end % of private function randomFrame.





function [bitIdx] = transformGrid(bitIdx)

% Transform a bit position from the standard grid into the other grid (linear
% bit positions for Motorola signals) and vice versa.
%   Return value bitIdx:
% Get the same bit position but expressed in the other grid.
%   Parameter bitIdx:
% The bit position to transform. This is a zero based index in one of the two grids.

    % The size of a byte in Bit.
    L = 8;

    % The computation is based on integer division.
    byteIdx = floor(bitIdx/L);
    remBit = bitIdx - L*byteIdx;
    assert(remBit < L)
    bitIdx = L*byteIdx + (L-1)-remBit;

end % of function transformGrid
