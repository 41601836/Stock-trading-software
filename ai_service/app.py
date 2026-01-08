from flask import Flask, request, jsonify
import akshare as ak
import pandas as pd

app = Flask(__name__)

# 配置跨域
from flask_cors import CORS
CORS(app)

@app.route('/api/v1/akshare/lhb/detail', methods=['GET'])
def get_lhb_detail():
    """获取东方财富龙虎榜详情"""
    try:
        stock_code = request.args.get('stock_code')
        date = request.args.get('date')
        
        if not stock_code or not date:
            return jsonify({
                'code': 400,
                'msg': '缺少必填参数 stock_code 或 date',
                'data': None
            })
        
        # 使用AkShare获取龙虎榜详情
        df = ak.stock_lhb_detail_em(symbol=stock_code, date=date)
        
        # 转换为JSON格式
        result = df.to_dict(orient='records')
        
        return jsonify({
            'code': 200,
            'msg': '成功',
            'data': result
        })
    except Exception as e:
        return jsonify({
            'code': 500,
            'msg': f'获取龙虎榜详情失败: {str(e)}',
            'data': None
        })

@app.route('/api/v1/akshare/margin/detail', methods=['GET'])
def get_margin_detail():
    """获取两融余量异动"""
    try:
        stock_code = request.args.get('stock_code')
        start_date = request.args.get('start_date')
        end_date = request.args.get('end_date')
        
        if not stock_code or not start_date or not end_date:
            return jsonify({
                'code': 400,
                'msg': '缺少必填参数 stock_code、start_date 或 end_date',
                'data': None
            })
        
        # 使用AkShare获取两融余量异动
        df = ak.stock_margin_detail_sznx(symbol=stock_code, start_date=start_date, end_date=end_date)
        
        # 转换为JSON格式
        result = df.to_dict(orient='records')
        
        return jsonify({
            'code': 200,
            'msg': '成功',
            'data': result
        })
    except Exception as e:
        return jsonify({
            'code': 500,
            'msg': f'获取两融余量异动失败: {str(e)}',
            'data': None
        })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)