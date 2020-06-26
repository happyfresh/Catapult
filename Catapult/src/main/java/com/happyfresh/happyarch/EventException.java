package com.happyfresh.happyarch;

public class EventException extends Exception {

    private Class tag;

    private Event event;

    public EventException(Throwable cause, Class tag, Event event) {
        super(cause);
        this.tag = tag;
        this.event = event;
    }

    public Class getTag() {
        return tag;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return super.toString() + ", tag: " + tag.getName() + ", event: " + event.toString();
    }
}
