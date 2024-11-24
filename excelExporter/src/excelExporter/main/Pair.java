/**
 * @file Pair.java
 * A helper class: A pair of objects.
 *
 * Copyright (C) 2015-2024 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class Pair
 *   Pair
 */

package excelExporter.main;

/**
 * Container to ease passing around a tuple of two objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.<p>
 *   <b>Remark:</b> This class has been copied from
 * http://stackoverflow.com/questions/5303539/didnt-java-once-have-a-pair-class as of
 * 6.3.2015
 *   @param <F>
 * The data type of the first element of the pair.
 *   @param <S>
 * The data type of the second element of the pair.
 */
public class Pair<F, S>
{
    /** The first element of the pair. */
    public final F first;
    
    /** The second element of the pair. */
    public final S second;

    /**
     * Constructor for a Pair.
     *   @param first
     * The first object in the pair.
     *   @param second 
     * The second object in the pair.
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o The {@link Pair} to which this one is to be checked for equality.
     * @return Get true if the underlying objects of the pair are both considered
     *         equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> p = (Pair<?, ?>)o;
        return first.equals(p.first) && second.equals(p.second);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Pair
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }

    /**
     * Convenience method for creating an appropriately typed pair.
     * @param <A> Type of the first object in the Pair.
     * @param <B> Type of the second object in the Pair.
     * @param a The first object in the pair.
     * @param b The second object in the pair.
     * @return Get a pair that is templatized with the types of a and b.
     */
    public static <A, B> Pair <A, B> create(A a, B b) {
        return new Pair<A, B>(a, b);
    }
} /* End of class Pair */



