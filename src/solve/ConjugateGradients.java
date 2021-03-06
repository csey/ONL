package solve;
import line.LineSearch;
import func.RealFunc;
import util.Vector;

/**
 * Non-linear variant of the conjugate gradients.
 * 
 * @author Gilles Chabert
 *
 */
public class ConjugateGradients extends Algorithm {
	
	private Vector dk ;	
	private RealFunc f;
	private LineSearch s;

	/**
	 * Build the algorithm for a given function and
	 * with an underlying line search technique.
	 * 
	 * @param f the function
	 * @param s the line search algorithm
	 */
	public ConjugateGradients(RealFunc f, LineSearch s) {
		
		this.f = f;
		this.s = s;
	
	}
	
	/**
	 * Start the iteration
	 */
	public void start(Vector x0) {
		super.start(x0);
		dk = f.grad(iter_vec).leftmul(-1);
	}
	
	/**
	 * Calculate the next iterate.
	 * 
	 * (update iter_vec).
	 */
	public void compute_next() throws EndOfIteration {
		double alpha = s.search(iter_vec,dk);
		Vector xk1 = iter_vec.add(dk.leftmul(alpha));
		//Mettre à jour iter_vec et dk
		double beta = ((Math.pow(f.grad(xk1).norm(), 2)) / (Math.pow(f.grad(iter_vec).norm(), 2))) ;
		dk = f.grad(xk1).leftmul(-1).add(dk.leftmul(beta));
		iter_vec = xk1;
	}
}
