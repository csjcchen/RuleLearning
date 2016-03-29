package gilp.utility;

public class KVPair<K, V> {
	
	private K _key;
	private V _value;
	
	public KVPair(K key, V val){
		this._key = key;
		this._value = val;
	}
	
	public K get_key() {
		return _key;
	}
	public void set_key(K key) {
		this._key = key;
	}
	public V get_value() {
		return _value;
	}
	public void set_value(V value) {
		this._value = value;
	}
}
