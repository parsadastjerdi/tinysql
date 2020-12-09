package storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Objects;

/**
 * A disk or memory block contains a number of records/tuples that
 * belong to the same relation.
 * A tuple CANNOT be split and stored in more than one blocks.
 * Each block is defined to hold as most FIELDS_PER_BLOCK fields.
 * Therefore, the max number of tuples held in a block can be
 * calculated from the size of a tuple, which is the number of
 * fields in a tuple. You can get the number by calling
 * Schema::getTuplesPerBlock().
 *
 * The max number of tuples held in a block =
 *       FIELDS_PER_BLOCK / num_of_fields_in_tuple
 *
 * Usage:
 *      Blocks already reside in the memory and disk. You don't need to
 *      create blocks manually. Most time when you need to use the Block
 *      class is to access a block of the main memory and to get or modify
 *      the tuples in the memory block. First be sure to get a pointer to
 *      a memory block from the Memory class.
 */

public class Block implements Serializable {
    private final ArrayList<Tuple> tuples;

    /**
     * This constructor is for internal use only, it is NOT needed
     * outside of the storage package. Use the blocks in memory or
     * relation directly.
     */
    protected Block() {
        tuples = new ArrayList<Tuple>();
    }

    /**
     * Copy constructor. Will copy over all tuples within the other
     * block.
     *
     * @param block: Block that is used to copy over tuples
     */
    protected Block(Block block) {
        tuples = new ArrayList<Tuple>();
        tuples.addAll(block.tuples);
    }

    /**
     * Gets the tuple at the tuple_index.
     *
     * @param tuple_offset
     *      Offset of tuple within
     * @return
     *      returns empty Tuple if tuple_index is out of bounds.
     */
    public Tuple getTuple(int tuple_offset) {
        if (!tuples.isEmpty() && tuple_offset >= tuples.get(0).getTuplesPerBlock()) {
            System.err.print("getTuple ERROR: tuple offet "
                    + tuple_offset + " out of bound of the block" + "\n");
            return new Tuple();
        }

        if (tuple_offset < 0 || tuple_offset >= tuples.size()) {
            System.err.print("getTuple ERROR: tuple offet "
                    + tuple_offset + " out of bound" + "\n");
            return new Tuple();
        }

        return new Tuple(tuples.get(tuple_offset));
    }

    // returns all the tuples inside this block
    public ArrayList<Tuple> getTuples() {
        //return (ArrayList<Tuple>) DeepCopy.copy(tuples);
        ArrayList<Tuple> tuples = new ArrayList<Tuple>(this.tuples.size());
        ListIterator<Tuple> lit = this.tuples.listIterator();
        while (lit.hasNext()) {
            tuples.add(new Tuple(lit.next()));
        }
        return tuples;
    }

    // sets new tuple value at tuple_index;
    // returns false if tuple_index out of bound
    public boolean setTuple(int tuple_offset, Tuple tuple) {
        Schema s = tuple.getSchema();
        if (!tuples.isEmpty()) {
            if (tuple_offset >= tuples.get(0).getTuplesPerBlock()) {
                System.err.print("setTuple ERROR: tuple offet "
                        + tuple_offset + " out of bound of the block" + "\n");
                return false;
            }
            for (int i = 0; i < tuples.size(); i++) {
                if (!s.equals(tuples.get(i).getSchema())) {
                    System.err.print("setTuple ERROR: tuples' schemas " +
                            "do not match" + "\n");
                    return false;
                }
            }
        }
        if (tuple_offset < 0 || tuple_offset >= s.getTuplesPerBlock()) {
            System.err.print("setTuple ERROR: tuple offset "
                    + tuple_offset + " out of bound" + "\n");
            return false;
        }
        if (tuple_offset >= tuples.size()) {
            //If there is a gap before the offset,
            // filled it with invalid tuples
            Tuple t = new Tuple(tuple.schema_manager, tuple.schema_index);
            t.invalidate();
            for (int i = tuples.size(); i < tuple_offset; i++) {
                tuples.add(t);
            }
            tuples.add(new Tuple(tuple));
        } else
            tuples.set(tuple_offset, new Tuple(tuple));
        return true;
    }

    // remove all the tuples; sets new tuples for the block;
    // returns false if number of input tuples exceeds the space limit
    public boolean setTuples(ArrayList<Tuple> tuples) {
        if (tuples.size() > tuples.get(0).getTuplesPerBlock()) {
            System.err.print("setTuples ERROR: number of tuples " +
                    "exceed space limit of the block" + "\n");
            return false;
        }
        //this.tuples=(ArrayList<Tuple>) DeepCopy.copy(tuples);
        this.tuples.clear();
        ListIterator<Tuple> lit = tuples.listIterator();
        while (lit.hasNext())
            this.tuples.add(new Tuple(lit.next()));
        return true;
    }

    // remove all the tuples; sets new tuples for the block;
    // returns false if number of input tuples exceeds the space limit
    public boolean setTuples(ArrayList<Tuple> tuples,
                             int start_index, int end_index) {
        if ((end_index - start_index) > tuples.get(0).getTuplesPerBlock()) {
            System.err.print("setTuples ERROR: number of tuples " +
                    "exceed space limit of the block" + "\n");
            return false;
        }
        this.tuples.clear();
        for (int i = start_index; i < end_index; i++) {
            this.tuples.add(new Tuple(tuples.get(i)));
        }
        return true;
    }

    /**
     * Appends one tuple to the end of the block. Will return false if
     * the total number of tuples exceeds the space limit.
     *
     * @param tuple
     *      Tuple to be appended to the block
     */
    public void appendTuple(Tuple tuple) {
        if (isFull()) {
            System.err.print("appendTuple ERROR: the block is full"
                    + "\n");
            return;
        }
        this.tuples.add(new Tuple(tuple));
    }

    // invalidates the tuple at the offset
    public boolean invalidateTuple(int tuple_offset) {
        if (tuple_offset < 0 || tuple_offset >= tuples.size()) {
            System.err.print("nullTuple ERROR: tuple offet "
                    + tuple_offset + " out of bound" + "\n");
            return false;
        }
        tuples.get(tuple_offset).invalidate();
        return true;
    }

    // empty all the tuples in the block
    public boolean invalidateTuples() {
        for (int i = 0; i < tuples.size(); i++) {
            tuples.get(i).invalidate();
        }
        return true;
    }

    public boolean isFull() {
        if (tuples.isEmpty()) return false;
        return tuples.size() == tuples.get(0).getTuplesPerBlock();
    }

    public boolean isEmpty() {
        return tuples.isEmpty();
    }

    public void clear() {
        tuples.clear();
    }

    /**
     * Must go through the list of tuples and only count the
     * tuples that are non-null.
     *
     * @return
     *      current number of tuples inside the block.
     */
    public long getNumTuples() {
        return tuples.stream()
                .filter(Objects::nonNull)
                .count();

    }

    public String toString() {
        String str = "";
        if (tuples.isEmpty()) return str;
        ListIterator<Tuple> lit = tuples.listIterator();
        Tuple t = lit.next();
        if (t.isNull())
            str += "(hole)";
        else {
            str += t.toString();
        }
        for (; lit.hasNext(); ) {
            t = lit.next();
            str += ("\n");
            if (t.isNull())
                str += "(hole)";
            else
                str += t.toString();
        }
        return str;
    }
}
