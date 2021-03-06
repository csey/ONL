package solve;

import util.Vector;
import util.Matrix;
import func.QuadraForm;
import func.RealFunc;

/**
 * Basic QuasiNewton algorithm for unconstrained minimization problem.
 */


public class QuasiNewton extends Algorithm {
	
	/**
	* Fonction F
	*/
	private RealFunc f;
	/**
	* Delta
	*/
	private double dt;
	/**
	* Matrice Q
	*/
	private Matrix A;
	/**
	* Matrice inverse Q
	*/
	private Matrix H;
	
	/**
	* Initial trust region size .
	*/
	public final static double DELTA_INIT = 1.0;
	/**
	* Ratio by which the region size is either
	* multiplied / divided
	*/
	public final static double DELTA_RATIO = 2.0;
	/**
	* Minimal trust region size .
	*/
	public final static double DELTA_MIN = 1e-10;
	/**
	* Maximal trust region size .
	*/
	public final static double DELTA_MAX = 10;
	/**
	* Ratio of actual / predicted reduction above which
	* adequacy of the quadratic model is considered as good .
	*/
	public final static double GOOD_ADEQUACY = 0.75;
	/**
	* Ratio of actual / predicted reduction under which
	* adequacy of the quadratic model is considered as poor.
	*/
	public final static double POOR_ADEQUACY = 0.25;
	/* *
	* Small curvature : means that the new " measurement " ( dx , dg )
	* is close to be linearly dependent of the n previous ones .
	* When the dot product between dx / dg and e is less than this value ,
	* the corresponding matrix Hk / Ak should not be updated .
	*/
	public final static double SMALL_CURVATURE = 1e-20;
	/* *
	* When the gradient norm is less than this value , the first - order
	* condition are considered to be fulfilled .
	*/
	public final static double GRADIENT_MIN_NORM = 1e-15;
	
	
	/**
	 * Build the algorithm
	 * 
	 * @param f function to minimize
	 * @param s underlying line search algorithm
	 */
	
	public QuasiNewton (RealFunc f) {
		this.f = f;	
	}
	
	/**
	 * Retourne le minimum des deux valeurs
	 */	
	public double min (double alpha, double beta){
		if(alpha >= beta) {return alpha;}
		else {return beta;}
	}
	
	/**
	 * Start the iteration
	 */
	public void start(Vector x0) {
		super.start(x0);
		this.dt = DELTA_INIT;
		this.A = Matrix.identity(x0.size());
		this.H = Matrix.identity(x0.size());		
	}
	
	public void compute_next() throws EndOfIteration {
		System.out.println("Matrice A");
		System.out.println(A.toString());
		System.out.println();
		System.out.println("Matrice inverse H");
		System.out.println(H.toString());
		System.out.println();
		
		Vector g=f.grad(iter_vec);	
		Vector b=g.leftmul(-1);
		QuadraForm Q =  new QuadraForm(A,b);	
	
		Vector d = H.mult(b);
		Vector xq = (iter_vec).add(d);
		
		if(Q.eval(xq) < Q.eval(iter_vec)){	
			if (xq.sub(iter_vec).norm()<= dt){
				//On garde le deplacement xq
			}else{
				d = g.leftmul(-(dt)/(g.norm()));
				double alpha = 0 ;
				double gQg = g.scalar(A.mult(g));
				if(gQg <= 0) {
					alpha = 1 ;
				}else {
					alpha = (Math.pow(g.norm(),3))/(dt * gQg);
					alpha = min (alpha,1);
				}
				Vector cauchy = (iter_vec).add(d.leftmul(alpha));
				// On s'attaque au dogleg
				// diff caracterise la direction suivant le segment cauchy-> xq
				Vector diff = xq.sub(cauchy);
				// on normalise diff
				diff = diff.leftmul(1/diff.norm());
				// Resoudre l'equation d'intersection segment sphere
				double varDogleg = Math.pow(diff.scalar(iter_vec.sub(cauchy)),2) - Math.pow(iter_vec.sub(cauchy).norm(),2) + Math.pow(dt,2);
				double distance = 0 ;
				if(varDogleg > 0 ) {
					distance = -diff.scalar(iter_vec.sub(cauchy)) + varDogleg;
				}else {distance = -diff.scalar(iter_vec.sub(cauchy));}
				// System.out.println("Dogled"+varDogleg);
				// System.out.println("Distance"+distance);
				Vector dogleg = cauchy.add(diff).leftmul(distance);
				// System.out.println("dogleg :  "+dogleg.toString());
				// System.out.println("cauchy :  "+cauchy.toString());
				xq = dogleg ;
			}
			
		}else {
			d = g.leftmul(-(dt)/(g.norm()));
			double alpha = 0 ;
			double gQg = g.scalar(A.mult(g));
			if(gQg <= 0) {
				alpha = 1 ;
			}else {
				alpha = (Math.pow(g.norm(),3))/(dt * gQg);
				alpha = min (alpha,1);
			}
			// Mis à jour direct du point cauchy dans xq
			xq = (iter_vec).add(d.leftmul(alpha));
		}
		
		System.out.println("REGION DE CONFIANCE = "+dt);
		// ----------------------------------------------------------------------------
		
		double adequation = (f.eval(iter_vec)-f.eval(xq))/(Q.eval(iter_vec.sub(iter_vec))-Q.eval(xq.sub(iter_vec)));
		if(adequation >= GOOD_ADEQUACY && dt < DELTA_MAX) {
			dt = DELTA_RATIO * dt ;			
		}else if(adequation <= POOR_ADEQUACY && dt > DELTA_MIN ) {
			dt = dt / DELTA_RATIO ;
		}
		// ----------------------------------------------------------------------------
		
		//mis a jour de A
		Vector Dx = xq.sub(iter_vec);
		Vector Dg = f.grad(xq).sub(g);
		Matrix correction ;
		Vector error = Dg.sub( A.mult(Dx));
		
		// System.out.println("A : Dx.scalar(error) = "+Dx.scalar(error));
		if(Math.abs(Dx.scalar(error)) > SMALL_CURVATURE) {
			correction = error.mult(error).leftmul(1/Dx.scalar(error)) ;
			A = A.add(correction);
		}//else{System.out.println("Trop proche de zero denominateur de mis a jour de A");}
		
		//mis a jour de H
		error = Dx.sub(H.mult(Dg));
		// System.out.println("H : Dg.scalar(error) = "+Dg.scalar(error));
			if(Math.abs(Dg.scalar(error)) > SMALL_CURVATURE) {
				correction = error.mult(error).leftmul(1/Dg.scalar(error));
			H = H.add(correction);
		}//else{System.out.println("Trop proche de zero denominateur de mis a jour de H");}
		
		// ----------------------------------------------------------------------------
		iter_vec = xq ;
		if(f.grad(xq).norm() <= GRADIENT_MIN_NORM || dt <= DELTA_MIN) {
			throw new EndOfIteration();
		}
		System.out.println("------------------------------------------------------------------------------------------------");
		
	}
	

}
