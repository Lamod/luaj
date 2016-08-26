package lamo.luaj.parser.ast;

abstract public class Stat extends Node {

    private Block owner;

    public Block getOwner() {
        return this.owner;
    }

    public void setOwner(Block owner) {
        this.owner = owner;
    }

    public String getIntend() {
        return this.owner == null ? "" : this.owner.getIntend();
    }

}

