/**
 * @file NumberMap.java
 * A StringTemplate V4 support object to support (modifiable) numbers. For C code
 * generation linear counters are often useful to shape lists and enumerating sets of
 * #defines. For these, we offer a data object, which is functional by side-effect: Every
 * time it is addressed it'll have another value, particularly rising integer values.
 * Simply integer arithmetics including comparisons is also supported.
 *
 * Copyright (C) 2015-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/* Interface of class NumberMap
 *   NumberMap
 *   isUnaryOperation
 *   getOperand
 *   containsKey
 *   get
 *   testParser
 *   NumberCommand
 */

package codeGenerator.dataModelListener;

import java.util.*;
import java.text.*;
import java.util.regex.*;
import org.antlr.v4.runtime.*;
import org.apache.log4j.*;
//import org.apache.logging.log4j.*;

import codeGenerator.dbcParser.*;


/**
 * A map holding several (modifiable) numbers to support code generation.
 */

public class NumberMap extends ST4CmdInterpreter<Object,Object>
                       implements IST4CmdListener<Object,Object>
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = Logger.getLogger(NumberMap.class.getName());

    /** A short string, which preceeds all messages to the application log. Use for
        localizing the logged information. */
    private String logContext_ = "";

    /** The error counter, which counts errors and warnings from number command
        interpretation. */
    private ErrorCounter errCnt_ = null;

    /** The enumeration of supported number commands. */
    static public enum Operation { read, set, get, add, sub, mul, div, sadd, ssub, smul
                                 , and, or, xor, not, sr, asr, sl
                                 , isGE, isLE, isL, isG, isE, isNE
                                 };

    /** A fragment of the regular expression, which expresses the alternative available
        commands. */
    static private String reOperations = ""
                                         + Operation.set
                                         + "|" + Operation.get
                                         + "|" + Operation.add
                                         + "|" + Operation.sub
                                         + "|" + Operation.mul
                                         + "|" + Operation.div
                                         + "|" + Operation.sadd
                                         + "|" + Operation.ssub
                                         + "|" + Operation.smul
                                         + "|" + Operation.and
                                         + "|" + Operation.or
                                         + "|" + Operation.xor
                                         + "|" + Operation.not
                                         + "|" + Operation.sr
                                         + "|" + Operation.asr
                                         + "|" + Operation.sl
                                         + "|" + Operation.isGE
                                         + "|" + Operation.isLE
                                         + "|" + Operation.isL
                                         + "|" + Operation.isG
                                         + "|" + Operation.isE
                                         + "|" + Operation.isNE;

    /** This regular expression is used to filter the reset command and its arguments. */
    static private String rePseudoNumberName = "^"
                                                + "(\\p{Alpha}\\p{Alnum}*)"
                                                + "("
                                                +  "_(" + reOperations + ")"
                                                +  "(_((-?\\p{Digit}+)(n)?"
                                                +      "|0x(\\p{XDigit}+)"
                                                +    ")"
                                                +   "|_(\\p{Alpha}\\p{Alnum}*)"
                                                +  ")?"
                                                + ")?"
                                                + "$";

    /** The group index to extract the number name from the commands. */
    static final int idxGrpNumName = 1;

    /** The group containing the optional operation from the command. */
    static final int idxGrpCmd = 2;

    /** The group index to extract the operation from the command. */
    static final int idxGrpOperation = 3;

    /** The group index to extract the absolute value of the literal numeric operand from
        the reset command. */
    static final int idxGrpOperand = 6;

    /** The group index to extract the sign of the literal numeric operand from the
        command. */
    static final int idxGrpSignOperand = 7;

    /** The group index to extract the number-by-name operand from the command. */
    static final int idxGrpHexOperand = 8;

    /** The group index to extract the number-by-name operand from the command. */
    static final int idxGrpOperandName = 9;

    /** This regular expression is used to filter the reset command and its arguments. */
    static private final Pattern rePatternPseudoNumberName =
                                                    Pattern.compile(rePseudoNumberName);

    /**
     * The details of an operation on a number are modelled by this sub-class.
     */
    private class NumberCommand
    {
        /** Overall parsing result of the command. The other information is trustable only
            if we have a true here. */
        public boolean isValid_ = false;

        /** The name of the addressed number. */
        public String name_ = null;

        /** The operation on the number. */
        public Operation operation_ = Operation.read;

        /** All operations (except for read and a few others) have either a literal signed
            integer or another map stored number as single operand. This is the literal
            value. It is applied if {@link #operandNum_} is null. */
        public long operand_ = 0;

        /** All operations (except for read and a few others) have either a literal signed
            integer or another map stored number as single operand. This is the name of the
            map stored number. If null then the literal value {@link #operand_} applies. */
        public String operandNum_ = null;


        /**
         * Creating a new object means to perform the parsing and to fill the public fields
         * with the parse result.<p>
         *   The key into the number map is interpreted as a combination of the name of the
         * addressed number and an optional operation on this number. This method does
         * parsing of the details.
         *   @param command The command string to be parsed.
         */
        public NumberCommand(String command)
        {
            Matcher re = rePatternPseudoNumberName.matcher(command);
            if(re.matches())
            {
                isValid_ = true;

                name_ = re.group(idxGrpNumName);
                assert name_ != null: "Expect number name as a non empty group";
                if(name_.equals("true") || name_.equals("false"))
                {
                    /* We have a few forbidden names. The Boolean constants are defined to
                       be numeric literals and can't be number names at the same time. */
                    isValid_ = false;
                    errCnt_.error();
                    _logger.error( logContext_
                                   + name_ + " has been found as name of a number in "
                                   + command + ". The Boolean constants must not be used"
                                   + " as number names"
                                 );
                }

                if(re.group(idxGrpCmd) != null)
                {
                    final String op = re.group(idxGrpOperation);
                    assert op != null: "Expect operation as a non empty group";
                    operation_ = Operation.valueOf(op);

                    final String decOperandVal = re.group(idxGrpOperand)
                               , hexOperandVal = re.group(idxGrpHexOperand)
                               , operandName = re.group(idxGrpOperandName);

                    /* Double check non presence of operand for unary operations. */
                    if(isUnaryOperation(operation_))
                    {
                        if(decOperandVal != null  || hexOperandVal != null
                           ||  operandName != null
                          )
                        {
                            isValid_ = false;
                            errCnt_.error();
                            _logger.error( logContext_
                                           + "Found unexpected operand in command string "
                                           + command + " for unary operator " + op
                                         );
                        }
                    }

                    if(decOperandVal != null)
                    {
                        /* The operand is given as a literal decimal number with optional
                           suffix m as negative sign. */
                        operandNum_ = null;

                        assert !decOperandVal.isEmpty()
                               : "Expect operand value as a non empty group";
                        final String sign = re.group(idxGrpSignOperand);
                        assert sign == null  ||  !sign.isEmpty()
                               : "Expect sign as a non empty group";
                        try
                        {
                            operand_ = Long.parseLong(decOperandVal);
                            if(re.group(idxGrpSignOperand) != null)
                            {
                                if(operand_ < 0)
                                {
                                    errCnt_.warning();
                                    _logger.warn( logContext_
                                                  + "Suspicious numeric operand "
                                                  + decOperandVal + sign
                                                  + " found in command string " + command
                                                  + ". A negative value is combined with"
                                                  + " the sign suffix"
                                                 );
                                    /* This can cause an overflow in case of the largest
                                       negative number. Overever, this is normal integer
                                       behavior and we already issed a warning on the
                                       unwanted construct - no additional
                                       check/error/correction is done about this. */
                                }
                                operand_ = -operand_;
                            }
                        }
                        catch(NumberFormatException ex)
                        {
                            isValid_ = false;
                            errCnt_.error();
                            _logger.error( logContext_
                                           + "Invalid numeric operand " + decOperandVal
                                           + " found in command string " + command
                                           + ". " + ex.getMessage()
                                         );
                        }
                    }
                    else if(hexOperandVal != null)
                    {
                        /* The operand is given as a literal hexadecimal number. */
                        operandNum_ = null;

                        assert !hexOperandVal.isEmpty()
                               : "Expect operand value as a non empty group";
                        try
                        {
                            /* These numbers will often be used to do binary operations.
                               The conversion needs to do this for all bits, i.e. it needs
                               to make the distinction between positive and negative values
                               because of Java's lack for unsigned integers.
                                 We exploit the fact that only Java's string parsing
                               reports an overflow, the binary arithmetics doesn't. We can
                               simply split the long string into two of those, convert
                               these and add both parts. The possible overflow into the
                               negative range it just what we need. */
                            if(hexOperandVal.length() == 16)
                            {
                                /* Splitting is done only if the number exactely has the
                                   critical length. Longer strings are purposely not
                                   handled in order to benefit from the thrown error
                                   feedback otherwise. */
                                operand_ = Long.parseLong(hexOperandVal.substring(1), 16)
                                           | (Long.parseLong(hexOperandVal.substring(0,1), 16)
                                              << 60
                                             );
                            }
                            else
                                operand_ = Long.parseLong(hexOperandVal, 16);
                        }
                        catch(NumberFormatException ex)
                        {
                            isValid_ = false;
                            errCnt_.error();
                            _logger.error( logContext_
                                           + "Invalid numeric operand 0x" + hexOperandVal
                                           + " found in command string " + command
                                           + ". " + ex.getMessage()
                                         );
                        }
                    }
                    else if(operandName != null)
                    {
                        /* The operand is given as reference (by name) to another stored
                           number or it is a Boolean constant. */
                        if(operandName.equals("true"))
                        {
                            /* The operand is given as a literal Boolean value true. This
                               value is implemented as number -1: This way the available
                               binary operators AND and OR and NOT behave at the same time
                               as Boolean operators. */
                            operand_ = -1;
                            operandNum_ = null;
                        }
                        else if(operandName.equals("false"))
                        {
                            /* The operand is given as a literal Boolean value false. This
                               value is implemented as number 0. */
                            operand_ = 0;
                            operandNum_ = null;
                        }
                        else
                        {
                            operand_ = 0;
                            operandNum_ = operandName;
                        }
                    }
                    else
                    {
                        /* The optional numeric operand is not present. The default value
                           depends on the operation. */
                        operandNum_ = null;
                        switch(operation_)
                        {
                        case set:
                        case isGE: case isLE: case isL: case isG: case isE: case isNE:
                            operand_ = 0;
                            break;

                        case add:
                        case sadd:
                        case sub:
                        case ssub:
                        case sl: case sr: case asr:
                            operand_ = 1;
                            break;

                        case mul:
                        case smul:
                        case div:
                            operand_ = 2; /* Useful for binary pattern 1,2,4,8,... */
                            break;

                        /* Use the non changing operand as default for the binary
                           operators. */
                        case and:
                            operand_ = -1;
                            break;

                        case or:
                        case xor:
                            operand_ = 0;
                            break;

                        /* A don't care value for those operators which don't actually use
                           an operand. */
                        case get:
                        case not:
                            operand_ = 0;
                            break;

                        case read:
                        default:
                            assert false: "Invalid operation " + operation_ + " found";
                            operand_ = 0;
                        }
                    }
                }
                else
                {
                    /* Just referencing the name of a number and not using any option
                       extension means to read the number's value. */
                    operation_ = Operation.read;
                    operand_ = 0;
                    operandNum_ = null;
                }
            }
            else
                isValid_ = false;

        } /* End NumberCommand */

    } /* End of NumberMap.NumberCommand */



    /**
     * This map acts on numbers, which are stored as values of the map. Here is the
     * definition of a stored number.
     */
    private static class Number
    {
        /** The current value of the number. */
        private long value_;

        /** The increment of the number, which is applied as post operation after each
            reading. */
        private long increment_;

        /** The multiplication of the number value, which is applied as post operation
            after each reading. */
        private long factor_;


        /**
         * Create a number with initial value, post increment and post multiplikation
         * factor.
         *   @param value The initial value, which is got with the first reading.
         *   @param postIncrement Increment of the number, which is applied as post
         * operation after each reading.
         *   @param postFactor Multiplication factor for the number value, which is
         * applied as post operation after each reading.
         */
        public Number(long value, long postIncrement, long postFactor)
        {
            value_ = value;
            increment_ = postIncrement;
            factor_ = postFactor;

        } /* End of Number */


        /**
         * Program the future post operation, which may affect subsequent readings.
         *   @param postIncrement New increment of the number, which is optionally applied
         * as post operation after future readings.
         *   @param postFactor New multiplication factor for the number value, which is
         * optionally applied as post operation after future readings.
         */
        public void setPostOperation(long postIncrement, long postFactor)
        {
            increment_ = postIncrement;
            factor_ = postFactor;

        } /* End of Number.setPostIncrement */


        /**
         * Clear a possibly active post operation.
         */
        public void clearPostOperation()
        {
            increment_ = 0;
            factor_ = 1;

        } /* End of clearPostOperation */


        /**
         * Set the value of the number for next reading.
         *   @param value The new value, which is got with the next reading.
         *   @param clearPostOp If true then the post operations defined so far are discarded.
         */
        public void set(long value, boolean clearPostOp)
        {
            value_ = value;
            if(clearPostOp)
                clearPostOperation();

        } /* End of Number.set */


        /**
         * Add a value. Possibly programmed post operations are inactived.
         *   @param addend The numeric value to add.
         */
        public void add(long addend)
        {
            value_ += addend;
            clearPostOperation();
        }


        /**
         * Subtract a value. Possibly programmed post operations are inactived.
         *   @param addend The numeric value to subtract.
         */
        public void sub(long addend)
        {
            value_ -= addend;
            clearPostOperation();
        }


        /**
         * Multiply with a numeric value. Possibly programmed post operations are inactived.
         *   @param factor The numeric operand.
         */
        public void mul(long factor)
        {
            value_ *= factor;
            clearPostOperation();
        }


        /**
         * (Integer) divide by a numeric value. Possibly programmed post operations are
         * inactived.
         *   @param dividend The numeric operand.
         */
        public void div(long dividend)
        {
            value_ /= dividend;
            clearPostOperation();
        }


        /**
         * Binary AND with a numeric value. Possibly programmed post operations are
         * inactived.
         *   @param binVal The numeric operand.
         */
        public void and(long binVal)
        {
            value_ &= binVal;
            clearPostOperation();
        }


        /**
         * Binary OR with a numeric value. Possibly programmed post operations are
         * inactived.
         *   @param binVal The numeric operand.
         */
        public void or(long binVal)
        {
            value_ |= binVal;
            clearPostOperation();
        }


        /**
         * Binary XOR with a numeric value. Possibly programmed post operations are
         * inactived.
         *   @param binVal The numeric operand.
         */
        public void xor(long binVal)
        {
            value_ ^= binVal;
            clearPostOperation();
        }


        /**
         * Binary NOT operation. Possibly programmed post operations are inactived.
         */
        public void not()
        {
            value_ ^= -1;
            clearPostOperation();
        }


        /**
         * Binary shift left. Possibly programmed post operations are inactived.
         *   @param distance The number of bits to shift. A negative value is interpreted
         * as binary right shift of same absolute distance.
         */
        public void sl(long distance)
        {
            if(distance < 0)
            {
                sr(-distance);
                return;
            }
            else if(distance < 64)
                value_ <<= distance;
            else
                value_ = 0;

            clearPostOperation();
        }


        /**
         * Binary shift right. Possibly programmed post operations are inactived.
         *   @param distance The number of bits to shift.
         */
        public void sr(long distance)
        {
            if(distance < 0)
            {
                sl(-distance);
                return;
            }
            else if(distance < 64)
                value_ >>>= distance;
            else
                value_ = 0;

            clearPostOperation();
        }


        /**
         * Arithmetic shift right. Possibly programmed post operations are inactived.
         *   @param distance The number of bits to shift.
         */
        public void asr(long distance)
        {
            if(distance < 0)
            {
                sl(-distance);
                return;
            }
            else if(distance < 64)
                value_ >>= distance;
            else
            {
                if(value_ >= 0)
                    value_ = 0;
                else
                    value_ = -1;
            }
            clearPostOperation();
        }


        /**
         * Read the current value and optionally do the post operations to modify the number.
         *   @return Get the integer value.
         *   @param doPostOp Apply the optional post operations to modify the number by
         * side-effect.
         */
        public long read(boolean doPostOp)
        {
            long result = value_;
            if(doPostOp)
            {
                value_ += increment_;
                value_ *= factor_;
            }
            return result;

        } /* End of Number.read */

    } /* End of NumberMap.Number */


    /** This is the actual map of numbers stored in this NumberMap. */
    private final Map<String,NumberMap.Number> mapNumberByName_;


    /**
     * Query the number of numbers currently stored in this number map. This overridden
     * implementation of the Map interface interprets the query as related to the map of
     * numbers - as opposed to the command interpreter we extend and which actually is a
     * Java Map.
     *   @return Get the number of numbers currently stored numbers.
     */
    @Override public int size()
        { return mapNumberByName_.size(); }
    

    /**
     * Logically, this NumberMap is a map of Number objects. Concerning Java types it
     * <b>is</b> a {@link ST4CmdInterpreter}, i.e. a Java Map, which is not used as such,
     * and it <b>has</b> the HashMap {@link #mapNumberByName_} of numbers. This means that
     * the calls of {@code this} will all relate to the wrong Java Map instance. We need
     * to override those methods of the Map interface, which are relevant to the
     * StringTemplate V4 engine.<p>
     *   {@link Map#entrySet} is applied by the template engine to iterate through all
     * elements of the map.
     *   @return Get the sorted set of logical map entries, i.e. the (name, number) pairs
     * currently stored in {@link #mapNumberByName_}. Sorted means they are lexically
     * sorted by name of the numbers. The iteration done by the template engine should
     * yield this order.
     */        
    @Override public Set<Map.Entry<String,Object>> entrySet()
    {
        /* We need to redirect the query for the pairs stored in the map from the command
           interpreter (the Java Map, we extend with this class) to the actual data map,
           the map of number objects. The types of both incorporated maps are not
           equivalent therefore it's not just calling the same function of the other map.
           We will have to build up the targeted set of entries. Doing this explicitly
           gives us the chance to sort the keys lexicographical - this is advantageous
           since the only meaningful application of map iteration from a StringTemplate V4
           template is the generation of an overview list of stored numbers. */
           
        /* Applying a tree set yields an iteration below in the for loop, which is done in
           the natural ordering of type String. */
        TreeSet<String> setOfKeys = new TreeSet<>(mapNumberByName_.keySet());

        /* The returned Map needs to retain the order of elements, which we generated by
           using a TreeMap for the iteration. A simple HashMap would destroy the gained
           ordering again. */
        LinkedHashSet<Map.Entry<String,Object>> setOfEntries =
                                                        new LinkedHashSet<>(setOfKeys.size());
        for(String numberName: setOfKeys)
        {
            setOfEntries.add(new AbstractMap.SimpleEntry<String,Object>
                                    ( numberName
                                    , mapNumberByName_.get(numberName)
                                    )
                            );
        }
        
        assert setOfEntries.size() == size();
        return setOfEntries;

    } /* End of entrySet */
    

    /**
     * Query if an operation has an operand or not.
     *   @return Get true for if no operand is permitted for the operation.
     *   @param op The tested operation.
     */
    private static boolean isUnaryOperation(Operation op)
    {
        return op == Operation.read  ||  op == Operation.get  ||  op == Operation.not;
        
    } /* End isUnaryOperation */
    
     
    /**
     * Get the value of the operand of a parsed command. This can be the literal number
     * (direct parse result) or the value of a referenced number stored in the map.
     *   @return The value of the operand is returned as numerical, references are resolved.
     *   @param cmd The successfully parsed command.
     */
    private long getOperand(NumberCommand cmd)
    {
        assert cmd.isValid_;
        if(isUnaryOperation(cmd.operation_))
        {
            /* The cmd uses a unary operator, which doesn't have an operand. */
            return 0;
        }
        else
        {
            long operand;
            if(cmd.operandNum_ != null)
            {
                /* The operand is another number. It needs to be already defined, we
                   don't want to create the object by side-effect. */
                Number operandNum = (Number)mapNumberByName_.get(cmd.operandNum_);
                if(operandNum != null)
                {
                    /* We read the number without applying the post operations on the
                       read operand; this would mean a hard to handle side-effect to
                       the template designer. */
                    operand = operandNum.read(/* doPostOp */ false);
                }
                else
                {
                    /* Operand should be available; give feedback on template design
                       error. */
                    operand = 0;
                    errCnt_.error();
                    _logger.error(logContext_
                                  + "Invalid use of number map: Operand " + cmd.operandNum_
                                  + " of operation " + cmd.operation_ + " on number "
                                  + cmd.name_ + " does not exist"
                                 );
                }
            }
            else
            {
                /* A literal value has been supplied by the template. */
                operand = cmd.operand_;
            }

            return operand;
            
        } /* End if(Operation with or without operand?) */
        
    } /* End of getOperand */



    /**
     * Exceute a command on a number.
     *   @return The value of an arithemic operation is returned as Long object, which is a
     * valid numeric attribute for the StringTemplate V4 engine. Comnparison results are
     * returned as Boolean object, which suuports conditional code in the StringTemplate V4
     * template.
     *   @param cmd The successfully parsed command.
     */
    private Object exec(NumberCommand cmd)
    {
        assert cmd.isValid_;

        /* Get the value of the operand. The unary operators don't require this but it
           doesn't harm neither. */
        long operand = getOperand(cmd);

        Number number = (Number)mapNumberByName_.get(cmd.name_);
        if(number != null)
        {
            switch(cmd.operation_)
            {
            case read:
                /* Normal operation: Read and return value, implicitly do the post
                   processing for the next reading. */
                return Long.valueOf(number.read(/* doPostOp */ true));

            case get:
                /* Read a number without applying the post operation and return the value. */
                return Long.valueOf(number.read(/* doPostOp */ false));

            case isGE:
                /* Comparison: Return a Java Boolean. Checking a number must not trigger
                   the post processing. */
                return Boolean.valueOf(number.read(/* doPostOp */ false) >= operand);

            case isLE:
                /* Comparison: Return a Java Boolean. Checking a number must not trigger
                   the post processing. */
                return Boolean.valueOf(number.read(/* doPostOp */ false) <= operand);

            case isG:
                /* Comparison: Return a Java Boolean. Checking a number must not trigger
                   the post processing. */
                return Boolean.valueOf(number.read(/* doPostOp */ false) > operand);

            case isL:
                /* Comparison: Return a Java Boolean. Checking a number must not trigger
                   the post processing. */
                return Boolean.valueOf(number.read(/* doPostOp */ false) < operand);

            case isE:
                /* Comparison: Return a Java Boolean. Checking a number must not trigger
                   the post processing. */
                return Boolean.valueOf(number.read(/* doPostOp */ false) == operand);

            case isNE:
                /* Comparison: Return a Java Boolean. Checking a number must not trigger
                   the post processing. */
                return Boolean.valueOf(number.read(/* doPostOp */ false) != operand);

            case set:
                /* Overwrite the current value for the next reading but don't change the
                   counting characteristics. */
                number.set(operand, /* clearPostOp */ false);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case add:
                number.add(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case sadd:
                number.add(operand);

                /* Activate the post increment operation. */
                number.setPostOperation(operand, 1);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case sub:
                number.sub(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case ssub:
                number.sub(operand);

                /* Activate the post increment operation. */
                number.setPostOperation(-operand, 1);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case smul:
                number.mul(operand);

                /* Activate the post multiplication operation. */
                number.setPostOperation(0, operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case mul:
                number.mul(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case div:
                number.div(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case and:
                number.and(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case or:
                number.or(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case xor:
                number.xor(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case not:
                number.not();

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case sl:
                number.sl(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case sr:
                number.sr(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            case asr:
                number.asr(operand);

                /* Return null; this is a silent operation with respect to the template
                   expansion in the template engine. */
                return null;

            default:
                assert false: "Invalid number operation " + cmd.operation_ + " encountered";
                return null;

            } /* switch(Which operation to apply to number?) */
        }
        else
        {
            /* This is the first time a StringTemplate template refers to this number.
               Normally we will create it now. However, to give better feedback on bad,
               likely unwanted template design, we only do this for some operations:
                 read: We begin with a null based, linear upcounter.
                 set: We assume a constant number without post operation. This is just an
               assignment.
                 sadd: We assume a null based upcounter. Initial value is null regardless
               of the numeric operand. This is somewhat inconsistent with the normal
               meaning of the sadd operation and with the other counters but supports the
               most relevant use case.
                 ssub: We assume a null based downcounter. Initial value is null regardless
               of the numeric operand. This is somewhat inconsistent with the normal
               meaning of the sadd operation but supports the most relevant use case.
                 smul: We assume a 1 based logarithmic upcounter. Initial value is one
               regardless of the numeric operand. This is somewhat inconsistent with the
               normal meaning of the smul operation but supports the most relevant use
               case.
                 Other operations and comparisons are reported as error. */
            long initVal, postInc, postFactor;
            switch(cmd.operation_)
            {
            case read:
                initVal = 0;
                postInc = 1;
                postFactor = 1;
                break;

            case set:
                initVal = operand;
                postInc = 0;
                postFactor = 1;
                break;

            case ssub:
                initVal = -operand;
                postInc = initVal;
                postFactor = 1;
                break;

            case sadd:
                initVal = operand;
                postInc = initVal;
                postFactor = 1;
                break;

            case smul:
                initVal = 1;
                postInc = 0;
                postFactor = operand;
                break;

            default:
                initVal = 0;
                postInc = 0;
                postFactor = 1;
                errCnt_.error();
                _logger.error(logContext_
                              + "Invalid use of number map: Operation "
                              + cmd.operation_ + " must not be used as first operation on"
                              + " number " + cmd.name_ + ". Value and characteristics"
                              + " of this number are still undefined"
                             );
            } /* switch(Which operation requires number instatiation?) */

            /* Create the new object and store it in this number map. */
            number = new Number(initVal, postInc, postFactor);
            mapNumberByName_.put(cmd.name_, number);

            /* Return null instead of a numeric value to the template engine for all
               modifying commands; only the read operation yields a result (and changes the
               value by post operations). */
            if(cmd.operation_ == Operation.read)
                return Long.valueOf(number.read(/* doPostOp */ true));
            else
                return null;

        } /* End if(New or existing number?) */

    } /* End of exec */



    /**
     * This method implements the command listener for the queries into or operations on
     * the number map.
     *   @return The value of the queried number or null if {@code cmd} is an
     * operation on the number or if it is an invalid number/command.
     *   @param context
     * The context is not used; the listener is connected only to a single command
     * interpreter. It expects null.
     *   @param cmdString
     * The command string. It is interpreted as command to access or modify a number.
     */
    public Object interpret(Object context, String cmdString)
    {
        assert context == null;

        /* The StringTemplate V4 engine first checks for the presence of the template
           requested key. If the requested key doesn't exist in the map then it queries the
           map nonetheless but with pseudo key "default". This key can thus be used to
           implement a default behavior - we could e.g. return the string "invalid number
           addressed". */

        NumberCommand cmd = new NumberCommand(cmdString);
        if(cmd.isValid_)
        {
            _logger.debug("get: Operation " + cmd.operation_
                          + (cmd.operation_ != Operation.read
                             ? "(" + (cmd.operandNum_ != null
                                      ? cmd.operandNum_
                                      : ""+cmd.operand_
                                     )
                               + ")"
                             : ""
                            )
                          + " is applied to number " + cmd.name_
                         );
            return exec(cmd);
        }
        else
        {
            /* The map is used with a key, which doesn't evaluate to a valid number
               command. We return null and the template expands to nothing. However, this
               is considered a design error of the template and we report it. */
            errCnt_.error();
            _logger.error(logContext_
                          + "Invalid use of number map: Command " + cmdString
                          + " doesn't evaluate to a valid numeric operation"
                         );
            return null;
        }
    } /* End of IST4CmdListener.interpret */


    /**
     * Create the number map.
     *   @param errCnt Number use related errors are counted in this object.
     *   @param logContext A brief piece of text, which preceeds all log messages of this
     * module.
     */
    public NumberMap(ErrorCounter errCnt, String logContext)
    {
        super( /* context */ null
             , /* cmdListener */ null
             , errCnt
             , /* logContext */ "NumberMap: "
             );
        super.attachListener(this);
        assert errCnt != null: "Don't pass null as error counter";
        errCnt_ = errCnt;
        logContext_ = logContext;
        mapNumberByName_ = new HashMap<String,NumberMap.Number>();
        
    } /* End constructor NumberMap */


    /**
     * Test pattern matching. The implementation uses the logger on info level, therefore
     * the method should solely be used for debugging purpose.
     *   @param input A string, which is handled as if it where the key value as passed in
     * by the StringTemplate V4 engine.
     */
    public void testCmdParser(String input)
    {
        _logger.info("testCmdParser: Check parsing of map key " + input);

        NumberCommand cmd = new NumberCommand(input);
        if(cmd.isValid_)
        {
            _logger.info("get: Operation " + cmd.operation_
                         + (cmd.operation_ != Operation.read
                            ? "(" + (cmd.operandNum_ != null
                                     ? cmd.operandNum_
                                     : ""+cmd.operand_
                                    )
                              + ")"
                            : ""
                           )
                         + " is applied to number " + cmd.name_
                        );
        }
        else
        {
            _logger.info("Key " + input + " is no valid number command");
        }
    } /* End testCmdParser */


    /**
     * Run the parser test on several literal commands.
     */
    public void testParser()
    {
        testCmdParser("cnt");
        testCmdParser("3*trash");
        testCmdParser("Gehts?");
        testCmdParser("aeh");
        testCmdParser("cnt0");
        testCmdParser("cnt1");
        testCmdParser("cnt1_set");
        testCmdParser("cnt1_set_1");
        testCmdParser("cnt1_set_23");
        testCmdParser("cnt1_set_23");
        testCmdParser("cnt1_add");
        testCmdParser("cnt1_add_1");
        testCmdParser("cnt1_sadd_1");
        testCmdParser("cnt1_add_23n"); // -23
        testCmdParser("cnt1_add_m23"); // not -23 but number "m23"
        testCmdParser("cnt1_mul");
        testCmdParser("cnt1_smul");
        testCmdParser("cnt1_smul_");
        testCmdParser("cnt1_smul_-34");
        testCmdParser("cnt1_smul_-35n");
        testCmdParser("cnt1_div");
        testCmdParser("bool_not");
        testCmdParser("bool_not_");
        testCmdParser("bool_not_false");
        testCmdParser("bool_not_4");
        testCmdParser("cnt1_mul_3");
        testCmdParser("cnt1_div_3");
        testCmdParser("cnt1_mul_1n");  // -1
        testCmdParser("cnt1_mul_myOtherNumber");
        testCmdParser("cnt1_mul_0x1");
        testCmdParser("cnt1_mul_0x1ffn");
        testCmdParser("cnt1_mul_0x1ff");
        testCmdParser("cnt1_isE_0x1ff");
        testCmdParser("cnt1_isNE_0x1ff");
        testCmdParser("cnt1_isG");
        testCmdParser("cnt1_isGE_1");
        testCmdParser("cnt1_isL_1");
        testCmdParser("cnt1_isLE");
        testCmdParser("xStickyCnt_get");
        testCmdParser("xStickyCnt_get_");
        testCmdParser("xStickyCnt_get_34");
        testCmdParser("xTooLittle_set_1n");
        testCmdParser("xTooLittle_set_1m");
        testCmdParser("true_set_1");
        testCmdParser("FALSE_set_0");

    } /* End testParser */

} /* End class NumberMap */
