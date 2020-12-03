public class Participant {
  private int value;
  private int cost;
  private boolean real;

  public Participant(int value, int cost, boolean real) {
    this.value = value;
    this.cost = cost;
    this.real = real;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public int getCost() {
    return cost;
  }

  public void setCost(int cost) {
    this.cost = cost;
  }

  public boolean isReal() {
    return real;
  }

  public void setReal(boolean real) {
    this.real = real;
  }

  @Override
  public String toString() {
    return "Participant{" + "value=" + value + ", cost=" + cost + ", real=" + real + '}';
  }
}
