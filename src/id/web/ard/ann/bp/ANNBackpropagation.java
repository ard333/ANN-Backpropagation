/*
 * Ardiansyah | http://ard.web.id
 */
package id.web.ard.ann.bp;

import java.util.ArrayList;
import java.util.Random;

/**
 * Artificial Neural Network with Backpropagation Training Algorithm.
 * 
 * @author Ardiansyah <ard333.ardiansyah@gmail.com>
 */
public final class ANNBackpropagation {
	
	private final Integer numOfInput;
	private final Integer numOfHidden;
	private final Integer numOfOutput;
	
	private final Double learningRate;
	private final Double minError;
	
	private Double[] X;//input
	private Double[] Y;//hidden
	private Double[] Z;//output
	
	private Double[][] w1;//input->hidden
	private Double[][] w2;//hidden->output
	
	private Double[] sigmaForY;
	private Double[] sigmaForZ;
	
	private Double[][] deltaw1;
	private Double[][] deltaw2;
	
	private Double[][] inputTraining;
	private Double[][] expectedOutput;
	
	private Integer epoch;
	private ActivationFunction activationFunction;
	
	private ArrayList<Double[][]> deltaw1History;
	private ArrayList<Double[][]> deltaw2History;
	private Integer windowSize;
	
	/**
	 * Create new Artificial Neural Network with specify parameters.
	 * 
	 * @param numOfInput number of input unit.
	 * @param numOfHidden number of hidden neuron.
	 * @param numOfOutput number of output neuron.
	 * @param learningRate learning rate (0.1 - 1).
	 * @param minError minimal error.
	 * @param activationFunction selected activation function.
	 * @param windowSize number of history weights changes stored, 0 if standard Update weights.
	 */
	public ANNBackpropagation(
			Integer numOfInput, Integer numOfHidden, Integer numOfOutput,
			Double learningRate, Double minError, ActivationFunction activationFunction,
			Integer windowSize
	) {
		this.numOfInput = numOfInput;
		this.numOfHidden = numOfHidden;
		this.numOfOutput = numOfOutput;
		this.learningRate = learningRate;
		this.minError = minError;
		this.activationFunction = activationFunction;
		this.windowSize = windowSize;
		if (this.windowSize > 0) {
			this.deltaw1History = new ArrayList<>();
			this.deltaw2History = new ArrayList<>();
		}
		this.init();
	}
	
	/**
	 * Initialize arrays and give random weights.
	 */
	private void init() {
		this.epoch = 0;
		
		this.X = new Double[numOfInput+1];
		this.Y = new Double[numOfHidden+1];
		this.Z = new Double[numOfOutput];
		this.X[numOfInput] = 1.0;//bias at last index
		this.Y[numOfHidden] = 1.0;//bias at last index
		
		this.sigmaForY = new Double[numOfHidden+1];
		this.sigmaForZ = new Double[numOfOutput];
		
		this.w1 = new Double[numOfInput+1][numOfHidden];
		this.w2 = new Double[numOfHidden+1][numOfOutput];
		this.deltaw1 = new Double[numOfInput+1][numOfHidden];
		this.deltaw2 = new Double[numOfHidden+1][numOfOutput];
		
		Random r = new Random();
		
		for (int i = 0; i < this.numOfInput+1; i++) {
			for (int j = 0; j < this.numOfHidden; j++) {
				this.w1[i][j] = -1 + (0 - (-1)) * r.nextDouble();//-1:1
			}
		}
		for (int i = 0; i < numOfHidden+1; i++) {
			for (int j = 0; j < numOfOutput; j++) {
				this.w2[i][j] = -1 + (0 - (-1)) * r.nextDouble();//-1:1
			}
		}
	}
	
	/**
	 * Set each pattern (Training Data) and Expected Output.
	 * 
	 * @param inputTraining set of training data.
	 * @param expectedOutput set of expected output.
	 */
	public void setTrainingData(Double[][] inputTraining, Double[][] expectedOutput) {
		this.inputTraining = inputTraining;
		this.expectedOutput = expectedOutput;
	}
	
	/**
	 * Train ANN until error minimum reached.
	 */
	public void train() {
		Double[] eO = new Double[numOfOutput];
		if (this.inputTraining!=null && this.expectedOutput!=null) {
			System.out.println("Learning Process, please wait...");
			Double err = 0.0;
			do {
				this.epoch++;
				for (int i = 0; i < this.inputTraining.length; i++) {
					System.arraycopy(this.inputTraining[i], 0, X, 0, this.inputTraining[i].length);
					System.arraycopy(this.expectedOutput[i], 0, eO, 0, this.expectedOutput[i].length);
					
					this.feedForward();
					this.backPropagation(eO);
				}
				err = this.caclERR();
				System.out.println("Error: "+err);
				//=============================
				if (this.windowSize > 0) {
					if (epoch > windowSize) {
						deltaw2History.remove(0);
						deltaw1History.remove(0);
					}
					deltaw2History.add(deltaw2);
					deltaw1History.add(deltaw1);
				}
				//=============================
			}while (err > this.minError);
		} else {
			System.out.println("No training data...");
		}
	}
	
	/**
	 * Calculate error average for all pattern.
	 * 
	 * @return error average.
	 */
	private Double caclERR() {
		Double[] eO = new Double[numOfOutput];
		Double err;
		Double errTotal = 0.0;
		
		for (int i = 0; i < this.inputTraining.length; i++) {
			err = 0.0;
			System.arraycopy(this.inputTraining[i], 0, X, 0, this.inputTraining[i].length);
			System.arraycopy(this.expectedOutput[i], 0, eO, 0, this.expectedOutput[i].length);
			this.feedForward();
			for (int a = 0; a < this.numOfOutput; a++) {
				err += Math.pow((eO[a]-this.Z[a]),2);
			}
			err /= numOfOutput;
			errTotal += err;
		}
		errTotal /= this.inputTraining.length;
		return errTotal;
	}
	
	/**
	 * Test pattern after training.
	 * 
	 * @param input input pattern.
	 */
	public void test(Double[] input) {
		System.arraycopy(input, 0, this.X, 0, this.numOfInput);
		this.feedForward();
	}
	
	/**
	 * Feed-forward.
	 */
	private void feedForward() {
		this.setOutputY();
		this.setOutputZ();
	}
	
	/**
	 * Calculate each output of hidden neuron.
	 */
	private void setOutputY() {
		for (int a = 0; a < numOfHidden; a++) {
			this.sigmaForY[a] = 0.0;
		}
		for (int j = 0; j < this.numOfHidden; j++) {
			for (int i = 0; i < this.numOfInput+1; i++) {
				this.sigmaForY[j] = this.sigmaForY[j] + this.X[i] * this.w1[i][j];
			}
		}
		for (int j = 0; j < numOfHidden; j++) {
			if (null != this.activationFunction) switch (this.activationFunction) {
				case SIGMOID:
					this.Y[j] = this.sigmoid(this.sigmaForY[j]);
					break;
				case BIPOLAR_SIGMOID:
					this.Y[j] = this.bipolarSigmoid(this.sigmaForY[j]);
					break;
				case TANH:
					this.Y[j] = this.tanH(this.sigmaForY[j]);
					break;
				default:
					break;
			}
		}
	}
	
	/**
	 * Calculate each output of output neuron.
	 */
	private void setOutputZ() {
		for (int a = 0; a < numOfOutput; a++) {
			this.sigmaForZ[a] = 0.0;
		}
		for (int k = 0; k < this.numOfOutput; k++) {
			for (int j = 0; j < this.numOfHidden+1; j++) {
				this.sigmaForZ[k] = this.sigmaForZ[k] + this.Y[j] * this.w2[j][k];
			}
		}
		for (int k = 0; k < this.numOfOutput; k++) {
			if (null != this.activationFunction) switch (this.activationFunction) {
				case SIGMOID:
					this.Z[k] = this.sigmoid(this.sigmaForZ[k]);
					break;
				case BIPOLAR_SIGMOID:
					this.Z[k] = this.bipolarSigmoid(this.sigmaForZ[k]);
					break;
				case TANH:
					this.Z[k] = this.tanH(this.sigmaForZ[k]);
					break;
				default:
					break;
			}
		}
	}
	
	/**
	 * Backpropagation.
	 * 
	 * @param expectedOutput set of expected output.
	 */
	private void backPropagation(Double[] expectedOutput) {
		Double[] fO = new Double[this.numOfOutput];
		
		for (int k = 0; k < numOfOutput; k++) {
			if (null != this.activationFunction) switch (this.activationFunction) {
				case SIGMOID:
					fO[k] = (expectedOutput[k]-this.Z[k]) * this.sigmoidDerivative(this.sigmaForZ[k]);
					break;
				case BIPOLAR_SIGMOID:
					fO[k] = (expectedOutput[k]-this.Z[k]) * this.bipolarSigmoidDerivative(this.sigmaForZ[k]);
					break;
				case TANH:
					fO[k] = (expectedOutput[k]-this.Z[k]) * this.tanHDerivative(this.sigmaForZ[k]);
					break;
				default:
					break;
			}
		}
		for (int j = 0; j < this.numOfHidden+1; j++) {//+bias weight
			for (int k = 0; k < this.numOfOutput; k++) {
				this.deltaw2[j][k] = this.windowedMomentumChanges(learningRate * fO[k] * this.Y[j], deltaw2History, j, k);
			}
		}
		Double[] fHNet = new Double[this.numOfHidden];
		for (int j = 0; j < this.numOfHidden; j++) {
			fHNet[j] = 0.0;
			for (int k = 0; k < this.numOfOutput; k++) {
				fHNet[j] = fHNet[j] + (fO[k]*this.w2[j][k]);
			}
		}
		Double[] fH = new Double[this.numOfHidden];
		for (int j = 0; j < this.numOfHidden; j++) {
			if (null != this.activationFunction) switch (this.activationFunction) {
				case SIGMOID:
					fH[j] = fHNet[j] * this.sigmoidDerivative(this.sigmaForY[j]);
					break;
				case BIPOLAR_SIGMOID:
					fH[j] = fHNet[j] * this.bipolarSigmoidDerivative(this.sigmaForY[j]);
					break;
				case TANH:
					fH[j] = fHNet[j] * this.tanHDerivative(this.sigmaForY[j]);
					break;
				default:
					break;
			}
		}
		for (int i = 0; i < this.numOfInput+1; i++) {
			for (int j = 0; j < numOfHidden; j++) {
				this.deltaw1[i][j] = this.windowedMomentumChanges(learningRate * fH[j] * this.X[i], deltaw1History, i, j);
			}
		}
		this.changeWeight();
	}
	
	/**
	 * Update all weights.
	 */
	private void changeWeight() {
		for (int j = 0; j < numOfHidden+1; j++) {
			for (int k = 0; k < numOfOutput; k++) {
				this.w2[j][k] = this.w2[j][k] + this.deltaw2[j][k];
			}
		}
		for (int i = 0; i < numOfInput+1; i++) {
			for (int j = 0; j < numOfHidden; j++) {
				this.w1[i][j] = this.w1[i][j] + this.deltaw1[i][j];
			}
		}
	}
	
	private Double windowedMomentumChanges(Double currentChanges, ArrayList<Double[][]> history, Integer a, Integer b) {
		Double temp = 0.0;
		if (this.windowSize > 0) {
			if (history.size()==this.windowSize) {
				for (Double[][] w : history) {
					temp += w[a][b];
				}
				if ((currentChanges * temp) < 0) {
					return 0.0;
				} else {
					return currentChanges;
				}
			} else {
				return currentChanges;
			}
		} else {
			return currentChanges;
		}
	}
	
	/**
	 * Sigmoid Activation Function.
	 * <br/>f(x) = 1 / (1 + exp(-x))
	 * 
	 * @param x an input value.
	 * @return a result of Sigmoid Activation Function.
	 */
	private Double sigmoid(Double x) {
		return 1 / (1 + (double)Math.exp(-x));
	}
	
	/**
	 * Derivative of Sigmoid Activation Function.
	 * <br/>f'(x) = f(x) * (1 - f(x))
	 * 
	 * @param x an input value.
	 * @return  a result of Derivative Sigmoid Activation Function.
	 */
	private Double sigmoidDerivative(Double x) {
		return this.sigmoid(x) * (1-this.sigmoid(x));
	}
	
	/**
	 * Sigmoid Bipolar Activation Function.
	 * <br/>f(x) = 2 / (1 + exp(-x)) - 1
	 * 
	 * @param x an input value.
	 * @return a result of Sigmoid Bipolar Activation Function.
	 */
	private Double bipolarSigmoid(Double x) {
		return 2/(1+Math.exp(-x))-1;
	}
	
	/**
	 * Derivative of Sigmoid Bipolar Activation Function.
	 * <br/>f'(x) = 0.5 * (1 + f(x)) * (1 - f(x))
	 * 
	 * @param x an input value.
	 * @return  a result of Derivative Sigmoid Bipolar Activation Function.
	 */
	private Double bipolarSigmoidDerivative(Double x) {
		return 0.5 * (1+this.bipolarSigmoid(x)) * (1-this.bipolarSigmoid(x));
	}
	
	/**
	 * TanH Activation Function.
	 * <br/>f(x) = 2 / (1 + exp(-x)) - 1
	 * <br/>output range -1 until 1.
	 * 
	 * @param x an input value.
	 * @return a result of TanH Activation Function.
	 */
	private Double tanH(Double x) {
		return 2/(1 + Math.exp(-2*x))-1;
	}
	
	/**
	 * Derivative of TanH Activation Function.
	 * <br/>f'(x) = 0.5 * (1 + f(x)) * (1 - f(x))
	 * <br/>output range -1 until 1.
	 * 
	 * @param x an input value.
	 * @return  a result of Derivative TanH Activation Function.
	 */
	private Double tanHDerivative(Double x) {
		return 1- Math.pow(this.tanH(x), 2);
	}
	
	/**
	 * Method for getting output of each output neuron.
	 * 
	 * @return output of each output neuron.
	 */
	public Double[] getOutput() {
		return this.Z;
	}
	
	/**
	 * Method for getting epoch until minimum error reached.
	 * 
	 * @return epoch until minimum error reached. 
	 */
	public Integer getEpoch() {
		return this.epoch;
	}
	
}
