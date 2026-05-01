package alice.mpatch.game;

public enum Side {
    CLIENT, SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isServer() {
        return this == SERVER;
    }


    @Override
    public String toString() {
        switch (this) {
            case CLIENT:
                return "Client";
            case SERVER:
                return "Server";
        }
        return super.toString();
    }
}
